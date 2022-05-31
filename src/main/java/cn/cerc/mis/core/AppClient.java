package cn.cerc.mis.core;

import java.io.Serializable;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.db.core.ISession;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.Utils;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppClient implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AppClient.class);
    private static final long serialVersionUID = -3593077761901636920L;

    public static final String CLIENT_ID = "CLIENTID";// deviceId, machineCode 表示同一个设备码栏位
    public static final String DEVICE = "device";

    // 手机
    public static final String phone = "phone";
    public static final String android = "android";
    public static final String iphone = "iphone";
    public static final String wechat = "weixin";
    // 平板
    public static final String pad = "pad";
    // 电脑
    public static final String pc = "pc";
    // 客户端专用浏览器
    public static final String ee = "ee";

    private String token; // application session id;
    private String deviceId; // device id
    private String device; // phone/pad/ee/pc
    private String languageId; // device language: cn/en
    private HttpServletRequest request;

    private String getValue(MemoryBuffer buff, String key, String def) {
        String result = def;
        String tmp = buff.getString(key);

        // 如果缓存有值，则从缓存中取值，且当def无值时，返回缓存值
        if (tmp != null && !"".equals(tmp)) {
            if (def == null || "".equals(def)) {
                result = tmp;
            }
        }

        // 如果def有值，且与缓存不同时，更新缓存
        if (def != null && !"".equals(def)) {
            if (tmp == null || !tmp.equals(def)) {
                buff.setValue(key, def);
            }
        }
        // 刷新缓存生命值
        try (Jedis redis = JedisFactory.getJedis()) {
            if (redis != null)
                redis.expire(buff.getKey(), buff.getExpires());
        }
        return result;
    }

    public String getId() {
        return this.deviceId == null ? Application.WebClient : this.deviceId;
    }

    public void setId(String value) {
        this.deviceId = value;
        request.setAttribute(CLIENT_ID, this.deviceId == null ? "" : this.deviceId);
        request.getSession().setAttribute(CLIENT_ID, value);
        if (value != null && value.length() == 28) {
            setDevice(phone);
        }

        if (token != null && this.deviceId != null && !"".equals(this.deviceId)) {
            try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.DeviceInfo, token)) {
                getValue(buff, CLIENT_ID, this.deviceId);
            }
        }
    }

    /**
     * 设备类型默认是 pc
     *
     * @return device
     */
    public String getDevice() {
        return this.device == null ? pc : device;
    }

    public void setDevice(String device) {
        if (device == null || "".equals(device)) {
            return;
        }

        // 更新类属性
        this.device = device;

        // 更新request属性
        request.setAttribute(DEVICE, device == null ? "" : device);
        request.getSession().setAttribute(DEVICE, device);

        // 更新设备缓存
        if (token != null) {
            try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.DeviceInfo, token)) {
                getValue(buff, DEVICE, device);
            }
        }
        return;
    }

    public String getLanguage() {
        return languageId == null ? LanguageResource.appLanguage : languageId;
    }

    public String getToken() {
        return "".equals(token) ? null : token;
    }

    /**
     * 清空token信息
     * <p>
     * TODO: 2019/12/7 考虑要不要加上缓存一起清空
     */
    public void clear() {
        if (!Utils.isEmpty(token)) {
            try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.DeviceInfo, token)) {
                buff.clear();
            }
            try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.SessionBase, token)) {
                buff.clear();
            }
        }
        this.token = null;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("token:").append(this.token).append(", ");
        buffer.append("deviceId:").append(this.deviceId).append(", ");
        buffer.append("deviceType:").append(this.device);
        return buffer.toString();
    }

    public boolean isPhone() {
        return phone.equals(getDevice()) || android.equals(getDevice()) || iphone.equals(getDevice())
                || wechat.equals(getDevice());
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public void setRequest(HttpServletRequest request) {
        // 保存设备类型
        this.request = request;
        this.device = request.getParameter(DEVICE);
        if (this.device == null || "".equals(this.device)) {
            this.device = (String) request.getSession().getAttribute(DEVICE);
        }
        if (this.device != null && !"".equals(this.device)) {
            request.getSession().setAttribute(DEVICE, this.device);
        }
        request.setAttribute(DEVICE, this.device == null ? "" : this.device);

        // 保存并取得 CLIENTID
        this.deviceId = request.getParameter(CLIENT_ID);
        if (this.deviceId == null || "".equals(this.deviceId)) {
            this.deviceId = (String) request.getSession().getAttribute(CLIENT_ID);
        }

        request.setAttribute(CLIENT_ID, this.deviceId);
        request.getSession().setAttribute(CLIENT_ID, this.deviceId);

        this.languageId = request.getParameter(ISession.LANGUAGE_ID);
        if (this.languageId == null || "".equals(this.languageId)) {
            this.languageId = (String) request.getSession().getAttribute(ISession.LANGUAGE_ID);
        }

        request.setAttribute(ISession.LANGUAGE_ID, this.languageId);
        request.getSession().setAttribute(ISession.LANGUAGE_ID, this.languageId);

        // 取得并保存token
        String token = request.getParameter(ISession.TOKEN);// 获取客户端的 token
        if (token == null || "".equals(token)) {
            token = (String) request.getSession().getAttribute(ISession.TOKEN); // 获取服务端的 token
            // 设置token
            if (Utils.isEmpty(token)) {
                log.debug("get token from request attribute is empty");
            } else {
                log.debug("get token from request attribute is {}", token);
            }
        }
        log.debug("request session id {}", request.getSession().getId());

        setToken(token);
    }

    /**
     * 设置token的值到session
     * 
     * @param value token的值
     */
    public void setToken(String value) {
        String token = Utils.isEmpty(value) ? null : value;
        if (token != null) {
            // 判断缓存是否过期
            try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.DeviceInfo, token)) {
                // 设备ID
                this.deviceId = getValue(buff, CLIENT_ID, this.deviceId);
                // 设备类型
                this.device = getValue(buff, DEVICE, this.device);
            }
        } else {
            if (this.token != null && !"".equals(this.token)) {
                log.warn("the param value is null，delete the token of cache: {}", this.token);
                MemoryBuffer.delete(SystemBuffer.Token.DeviceInfo, this.token);
            }
        }
        log.debug("sessionID 2: {}", request.getSession().getId());

        this.token = token;
        request.getSession().setAttribute(ISession.TOKEN, this.token);
        request.setAttribute(ISession.TOKEN, this.token == null ? "" : this.token);
    }

    /**
     * 获取客户端真实IP地址，不直接使用request.getRemoteAddr() 的原因是有可能用户使用了代理软件方式避免真实IP地址
     * <p>
     * x-forwarded-for 是一串IP值，取第一个非unknown的有效IP字符串为客户端的真实IP
     * <p>
     * 如：x-forwarded-for：192.168.1.110, 192.168.1.120, 192.168.1.130, 192.168.1.100
     * <p>
     * 用户真实IP为： 192.168.1.110
     *
     * @param request HttpServletRequest
     * 
     * @return IP地址
     */
    public static String getClientIP(HttpServletRequest request) {
        if (request == null)
            return "";
        try {
            String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
                ip = request.getHeader("Proxy-Client-IP");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
                ip = request.getHeader("WL-Proxy-Client-IP");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
                ip = request.getHeader("HTTP_CLIENT_IP");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
                ip = request.getRemoteAddr();
            if ("0:0:0:0:0:0:0:1".equals(ip))
                ip = "0.0.0.0";
            // 以第一个IP地址为用户的真实地址
            String arr[] = ip.split(",");
            ip = Arrays.stream(arr).findFirst().orElse("").trim();
            return ip;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}