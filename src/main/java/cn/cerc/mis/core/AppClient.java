package cn.cerc.mis.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.db.core.ISession;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.Utils;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.redis.RedisRecord;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppClient implements Serializable {
    private static final long serialVersionUID = -3593077761901636920L;

    // 缓存版本
    public static final int Version = 1;

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
    // 看板
    public static final String kanban = "kanban";
    // 客户端专用浏览器
    public static final String ee = "ee";

    private String token; // application session id;
    private String deviceId; // device id
    private String device; // phone/pad/ee/pc
    private String languageId; // device language: cn/en
    private HttpServletRequest request;

    /**
     * 根据 request 构建访问设备信息
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;

        Map<String, String> items = new HashMap<>();
        this.device = request.getParameter(DEVICE);
        if (!Utils.isEmpty(this.device))
            items.put(AppClient.DEVICE, this.device);

        this.deviceId = request.getParameter(CLIENT_ID);
        if (!Utils.isEmpty(this.deviceId))
            items.put(AppClient.CLIENT_ID, this.deviceId);

        this.languageId = request.getParameter(ISession.LANGUAGE_ID);
        if (!Utils.isEmpty(this.languageId))
            items.put(ISession.LANGUAGE_ID, this.languageId);

        this.token = request.getParameter(ISession.TOKEN);// 获取客户端的 token
        if (!Utils.isEmpty(this.token))
            items.put(ISession.TOKEN, this.token);

        // 将设备信息写入缓存并设置超时时间
        String key = MemoryBuffer.buildObjectKey(AppClient.class, request.getSession().getId(), Version);
        if (items.size() > 0) {
            try (Jedis redis = JedisFactory.getJedis()) {
                redis.hmset(key, items);
                redis.expire(key, RedisRecord.TIMEOUT);
            }
        }

        // 一次性从缓存中取值
        try (Jedis redis = JedisFactory.getJedis()) {
            this.device = redis.hget(key, AppClient.DEVICE);
            this.deviceId = redis.hget(key, AppClient.CLIENT_ID);
            this.languageId = redis.hget(key, ISession.LANGUAGE_ID);
            this.token = redis.hget(key, ISession.TOKEN);
        }

        // 往当前的request写入设备信息
        request.setAttribute(DEVICE, this.device);
        request.setAttribute(CLIENT_ID, this.deviceId);
        request.setAttribute(ISession.LANGUAGE_ID, this.languageId);
        request.setAttribute(ISession.TOKEN, this.token);
    }

    public String getId() {
        return this.deviceId == null ? Application.WebClient : this.deviceId;
    }

    public void setId(String value) {
        value = value == null ? "" : value;
        this.deviceId = value;
        request.setAttribute(CLIENT_ID, this.deviceId);
        String key = MemoryBuffer.buildObjectKey(AppClient.class, request.getSession().getId(), Version);
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.hset(key, AppClient.CLIENT_ID, device);
        }
        if (value != null && value.length() == 28)// 微信openid的长度
            setDevice(phone);
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
        if (Utils.isEmpty(device))
            return;
        // 更新类属性
        this.device = device;
        // 更新request属性
        request.setAttribute(DEVICE, device);
        // 更新缓存属性
        String key = MemoryBuffer.buildObjectKey(AppClient.class, request.getSession().getId(), Version);
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.hset(key, AppClient.DEVICE, device);
        }
        return;
    }

    public String getLanguage() {
        return languageId == null ? LanguageResource.appLanguage : languageId;
    }

    public String getToken() {
        return "".equals(token) ? null : token;
    }

    public void clear() {
        String key = MemoryBuffer.buildObjectKey(AppClient.class, request.getSession().getId(), Version);
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.del(key);
        }
        this.token = null;
    }

    public boolean isPhone() {
        return phone.equals(getDevice()) || android.equals(getDevice()) || iphone.equals(getDevice())
                || wechat.equals(getDevice());
    }

    public boolean isKanban() {
        return kanban.equals(getDevice());
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("token: ").append(this.token).append(", ");
        builder.append("deviceId: ").append(this.deviceId).append(", ");
        builder.append("deviceType: ").append(this.device);
        return builder.toString();
    }

}
