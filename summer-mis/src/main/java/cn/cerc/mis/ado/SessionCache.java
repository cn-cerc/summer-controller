package cn.cerc.mis.ado;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.core.DataRow;
import cn.cerc.mis.core.Application;

@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
public class SessionCache {
    private static final Logger log = LoggerFactory.getLogger(SessionCache.class);
    public Map<String, DataRow> items = new ConcurrentHashMap<>();

    public static void set(Object[] keys, DataRow value) {
        assert keys.length > 0;
        assert value != null;
        SessionCache sc = Application.getBean(SessionCache.class);
        String cacheKey = EntityCache.joinToKey(keys);
        log.debug("set: {}", cacheKey);
        sc.items.put(cacheKey, value);
    }

    public static DataRow get(Object[] keys) {
        assert keys.length > 0;
        SessionCache sc = Application.getBean(SessionCache.class);
        String cacheKey = EntityCache.joinToKey(keys);
        log.debug("get: {}", cacheKey);
        return sc.items.get(cacheKey);
    }

    public static void del(Object[] keys) {
        assert keys.length > 0;
        SessionCache sc = Application.getBean(SessionCache.class);
        String cacheKey = EntityCache.joinToKey(keys);
        log.debug("del: {}", cacheKey);
        sc.items.remove(cacheKey);
    }

}
