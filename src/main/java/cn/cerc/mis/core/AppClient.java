package cn.cerc.mis.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;

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
    private static final Logger log = LoggerFactory.getLogger(AppClient.class);

    private static final long serialVersionUID = -3593077761901636920L;

    // 缓存版本
    public static final int Version = 1;

    public static final String CLIENT_ID = "CLIENTID";// deviceId, machineCode 表示同一个设备码栏位
    public static final String DEVICE = "device";
    public static final String LANGUAGE = "language";

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

    private HttpServletRequest request;

    public AppClient(HttpServletRequest request) {
        this.request = request;
    }

    public static final String buildKey(String token) {
        if (Utils.isEmpty(token))
            return "";
        return MemoryBuffer.buildObjectKey(AppClient.class, token, AppClient.Version);
    }

    private static String getTooken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return "";
        Cookie cookie = Stream.of(cookies).filter(item -> ISession.TOKEN.equals(item.getName())).findAny().orElse(null);
        if (cookie == null)
            return "";
        String cookieId = cookie.getValue();
        if (Utils.isEmpty(cookieId))
            return "";
        return cookieId;
    }

    private static final String key(HttpServletRequest request) {
        String cookieId = getTooken(request);
        if (Utils.isEmpty(cookieId))
            return "";
        return AppClient.buildKey(cookieId);
    }

    public static final String value(HttpServletRequest request, String field) {
        String cookieId = getTooken(request);
        String key = AppClient.buildKey(cookieId);
        String value = request.getParameter(field);
        if (!Utils.isEmpty(value)) {
            try (Jedis redis = JedisFactory.getJedis()) {
                if (!Utils.isEmpty(key))
                    redis.hset(key, field, value);
            }
            return value;
        }
        if (Utils.isEmpty(key)) {
            log.warn("cookie field {} value is empty", field);
            return "";
        }
        // 如果 cookieId 等于token直接取值
        if (ISession.TOKEN.equals(field) && !Utils.isEmpty(cookieId)) {
            return cookieId;
        }
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.expire(key, RedisRecord.TIMEOUT);// 每次取值延长生命值
            return redis.hget(key, field);
        }
    }

    public static final Long setValue(HttpServletRequest request, String field, String value) {
        String key = AppClient.key(request);
        if (Utils.isEmpty(key)) {
            log.warn("cookie field {} value is empty", field);
            return 0L;
        }
        try (Jedis redis = JedisFactory.getJedis()) {
            return redis.hset(AppClient.key(request), field, value);
        }
    }

    public static final String msetValue(HttpServletRequest request, final Map<String, String> hash) {
        String key = AppClient.key(request);
        if (Utils.isEmpty(key)) {
            log.warn("cookie field {} value is empty", ISession.TOKEN);
            return "";
        }
        try (Jedis redis = JedisFactory.getJedis()) {
            return redis.hmset(AppClient.key(request), hash);
        }
    }

    public static final Long del(HttpServletRequest request, String field) {
        String key = AppClient.key(request);
        if (Utils.isEmpty(key)) {
            log.warn("cookie field {} value is empty", field);
            return 0L;
        }
        try (Jedis redis = JedisFactory.getJedis()) {
            return redis.del(AppClient.key(request), field);
        }
    }

    public String getId() {
        return AppClient.value(this.request, AppClient.CLIENT_ID);
    }

    public void setId(String value) {
        String clientId = value == null ? "" : value;
        request.setAttribute(AppClient.CLIENT_ID, clientId);
        AppClient.setValue(request, AppClient.CLIENT_ID, clientId);
        if (value != null && value.length() == 28)// 微信openid的长度
            setDevice(phone);
    }

    /**
     * 设备类型默认是 pc
     */
    public String getDevice() {
        String device = AppClient.value(this.request, AppClient.DEVICE);
        return Utils.isEmpty(device) ? pc : device;
    }

    public void setDevice(String device) {
        if (Utils.isEmpty(device))
            return;
        request.setAttribute(AppClient.DEVICE, device);
        AppClient.setValue(request, AppClient.DEVICE, device);
        return;
    }

    public String getLanguage() {
        String languageId = AppClient.value(this.request, AppClient.LANGUAGE);
        return Utils.isEmpty(languageId) ? LanguageResource.appLanguage : languageId;
    }

    public String getToken() {
        String token = AppClient.value(this.request, ISession.TOKEN);
        return Utils.isEmpty(token) ? null : token;
    }

    public void clear(String cookId) {
        if (Utils.isEmpty(cookId))
            return;
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.del(AppClient.buildKey(cookId));
        }
    }

    /**
     * 根据 request 构建访问设备信息
     */
    public static void setRequest(HttpServletRequest request) {
        Map<String, String> items = new HashMap<>();
        String device = request.getParameter(DEVICE);
        if (!Utils.isEmpty(device))
            items.put(AppClient.DEVICE, device);

        String deviceId = request.getParameter(CLIENT_ID);
        if (!Utils.isEmpty(deviceId))
            items.put(AppClient.CLIENT_ID, deviceId);

        String language = request.getParameter(AppClient.LANGUAGE);
        if (!Utils.isEmpty(language))
            items.put(AppClient.LANGUAGE, language);

        String token = request.getParameter(ISession.TOKEN);// 获取客户端的 token
        if (!Utils.isEmpty(token))
            items.put(ISession.TOKEN, token);

        // 将设备信息写入缓存并设置超时时间
        String key = AppClient.key(request);
        if (items.size() > 0) {
            try (Jedis redis = JedisFactory.getJedis()) {
                redis.hmset(key, items);
                redis.expire(key, RedisRecord.TIMEOUT);
            }
        }
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
        Map<String, String> items;
        try (Jedis redis = JedisFactory.getJedis()) {
            String key = AppClient.key(this.request);
            items = redis.hgetAll(key);
        }
        return new Gson().toJson(items);
    }

}
