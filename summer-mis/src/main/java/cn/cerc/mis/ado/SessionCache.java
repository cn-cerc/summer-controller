package cn.cerc.mis.ado;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.core.DataRow;
import cn.cerc.mis.core.Application;

@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
public class SessionCache {
    private static final Logger log = LoggerFactory.getLogger(SessionCache.class);
    public final Map<String, DataRow> items = new ConcurrentHashMap<>();

    public static void set(Object[] keys, DataRow value) {
        assert keys.length > 0;
        assert value != null;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = EntityCache.joinToKey(keys);
            log.debug("set: {}", cacheKey);
            sc.items.put(cacheKey, value);
        } catch (BeanCreationException e) {
            return;
        }
    }

    public static DataRow get(Object[] keys) {
        assert keys.length > 0;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = EntityCache.joinToKey(keys);
            log.debug("get: {}", cacheKey);
            return sc.items.get(cacheKey);
        } catch (BeanCreationException e) {
            return null;
        }

    }

    public static void del(Object[] keys) {
        assert keys.length > 0;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = EntityCache.joinToKey(keys);
            log.debug("del: {}", cacheKey);
            sc.items.remove(cacheKey);
        } catch (BeanCreationException e) {
            return;
        }
    }

    public void setItem(Object[] keys, DataRow value) {
        items.put(EntityCache.joinToKey(keys), value);
    }

    public DataRow getItem(Object[] keys) {
        return items.get(EntityCache.joinToKey(keys));
    }

    public void delItem(Object[] keys) {
        items.remove(EntityCache.joinToKey(keys));
    }

}
