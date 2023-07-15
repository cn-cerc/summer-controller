package cn.cerc.mis.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;

import cn.cerc.db.core.ISession;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.redis.RedisRecord;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppClient implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AppClient.class);

    @Serial
    private static final long serialVersionUID = -3593077761901636920L;

    // 缓存版本
    public static final int Version = 1;
    public static final String COOKIE_ROOT_PATH = "/";

    // 手机
    public static final String phone = "phone";
    public static final String android = "android";
    public static final String iphone = "iphone";
    public static final String wechat = "weixin";

    // GPS
    public static final String gps_pkg = ServerConfig.INSTANCE.getProperty("app.gps.pkgId", "");

    /**
     * 类手机终端
     */
    public static final List<String> phone_devices = new ArrayList<>();

    static {
        phone_devices.add(AppClient.phone);
        phone_devices.add(AppClient.android);
        phone_devices.add(AppClient.iphone);
        phone_devices.add(AppClient.wechat);
    }

    private static final List<String> browsers = new ArrayList<>();
    static {
        browsers.add("chrome");
        browsers.add("edge");
        browsers.add("mozilla");
        browsers.add("firefox");
        browsers.add("safari");
        browsers.add("apicloud");
        browsers.add("DitengApp");
        browsers.add("DitengAppPad");
    }

    // 平板
    public static final String pad = "pad";
    // 电脑
    public static final String pc = "pc";
    // 看板
    public static final String kanban = "kanban";
    // 客户端专用浏览器
    public static final String ee = "ee";

    private final HttpServletRequest request;

    private final String cookieId;

    private final String key;

    private String token;

    private String device;

    private String deviceId;

    private String pkgId;

    private String language;

    public AppClient(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;

        Variant variant = new Variant();
        AppClient.createCookie(request, response, variant);
        this.cookieId = variant.getString();

        this.key = MemoryBuffer.buildObjectKey(AppClient.class, this.cookieId, AppClient.Version);

        // 如果是非浏览器请求（CUrl接口）则不生成缓存信息
        String userAgent = request.getHeader("User-Agent");
        if (Utils.isEmpty(userAgent))
            return;
        if (browsers.stream().noneMatch(item -> userAgent.toLowerCase().contains(item.toLowerCase()))) {
            log.warn("User-Agent -> {}", userAgent);
            return;
        }

        Cookie[] cookies = request.getCookies();
        try (Jedis redis = JedisFactory.getJedis()) {
            this.device = request.getParameter(ISession.CLIENT_DEVICE);
            if (!Utils.isEmpty(device))
                redis.hset(key, ISession.CLIENT_DEVICE, device);
            else {
                this.device = redis.hget(key, ISession.CLIENT_DEVICE);
                if (Utils.isEmpty(device)) {
                    device = pc;
                    redis.hset(key, ISession.CLIENT_DEVICE, device);
                }
            }

            this.deviceId = request.getParameter(ISession.CLIENT_ID);
            if (!Utils.isEmpty(deviceId))
                redis.hset(key, ISession.CLIENT_ID, deviceId);
            else {
                this.deviceId = redis.hget(key, ISession.CLIENT_ID);

                if (Utils.isEmpty(deviceId)) {
                    if (cookies != null) {
                        for (Cookie cookie : request.getCookies()) {
                            if (cookie.getName().equals(ISession.CLIENT_ID)) {
                                this.deviceId = cookie.getValue();
                                break;
                            }
                        }
                    }
                }
            }

            this.pkgId = request.getParameter(ISession.PKG_ID);
            if (!Utils.isEmpty(pkgId))
                redis.hset(key, ISession.PKG_ID, pkgId);
            else
                this.pkgId = redis.hget(key, ISession.PKG_ID);

            this.language = request.getParameter(ISession.LANGUAGE_ID);
            if (!Utils.isEmpty(language))
                redis.hset(key, ISession.LANGUAGE_ID, language);
            else {
                this.language = redis.hget(key, ISession.LANGUAGE_ID);
                if (Utils.isEmpty(language)) {
                    language = LanguageResource.appLanguage;
                    redis.hset(key, ISession.LANGUAGE_ID, language);
                }
            }

            this.token = request.getParameter(ISession.TOKEN);
            if (!Utils.isEmpty(token))
                redis.hset(key, ISession.TOKEN, token);
            else
                this.token = redis.hget(key, ISession.TOKEN);

            redis.expire(key, RedisRecord.TIMEOUT);// 每次取值延长生命值
        } catch (IllegalStateException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 根据 request 生成 cookieId
     */
    public static boolean createCookie(HttpServletRequest request, HttpServletResponse response, Variant variant) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(ISession.COOKIE_ID)) {
                    variant.setValue(cookie.getValue());
                    break;
                }
            }
        }

        if (!variant.isModified()) {
            String cookieId = Utils.getGuid();
            Cookie cookie = new Cookie(ISession.COOKIE_ID, cookieId);
            cookie.setPath(COOKIE_ROOT_PATH);
            cookie.setHttpOnly(true);
            if (response != null)
                response.addCookie(cookie);
            variant.setValue(cookieId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 读取 cookie 中的 id
     */
    public String getCookieId() {
        return this.cookieId;
    }

    public String key() {
        return this.key;
    }

    public String getToken() {
        return token;
    }

    public void delete(String field) {
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.hdel(key, field);
        }
    }

    public String getId() {
        return this.deviceId;
    }

    public void setId(String value) {
        this.deviceId = value == null ? "" : value;
        request.setAttribute(ISession.CLIENT_ID, deviceId);
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.hset(key, ISession.CLIENT_ID, deviceId);
        }
        if (value != null && value.length() == 28)// 微信openid的长度
            setDevice(phone);
    }

    /**
     * 设备类型默认是 pc
     */
    public String getDevice() {
        return Utils.isEmpty(device) ? pc : device;
    }

    public void setDevice(String value) {
        this.device = Utils.isEmpty(value) ? pc : value;
        request.setAttribute(ISession.CLIENT_DEVICE, device);

        try (Jedis redis = JedisFactory.getJedis()) {
            redis.hset(key, ISession.CLIENT_DEVICE, device);
        }
    }

    public String getPkgId() {
        return this.pkgId;
    }

    public String getLanguage() {
        return this.language;
    }

    public boolean isPhone() {
        return phone_devices.contains(getDevice());
    }

    public boolean isKanban() {
        return kanban.equals(getDevice());
    }

    /**
     * 判断当前设备是否是pad
     * 
     * @return true
     */
    public boolean isPad() {
        return pad.equals(getDevice());
    }

    /**
     * 检查当前的token设备是否是GPS应用
     */
    public boolean isGPS() {
        if (Utils.isEmpty(this.getPkgId()))
            return false;
        return gps_pkg.contains(this.getPkgId());
    }

    /**
     * 检查当前的token设备是否是GPS应用
     */
    public static boolean isGPS(String pkgId) {
        if (Utils.isEmpty(pkgId))
            return false;
        return gps_pkg.contains(pkgId);
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
            String[] arr = ip.split(",");
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
            items = redis.hgetAll(key);
        }
        return new Gson().toJson(items);
    }

}
