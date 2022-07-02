package cn.cerc.mis.ado;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.db.core.DataRow;
import cn.cerc.mis.core.Application;

@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
public class SessionCache {
    private static final Logger log = LoggerFactory.getLogger(SessionCache.class);
    private static boolean createError = false;
    public final Map<String, DataRow> items = new ConcurrentHashMap<>();

    public static void set(String[] keys, DataRow value) {
        if (createError)
            return;
        assert keys.length > 0;
        assert value != null;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = String.join(".", keys);
            log.debug("set: {}", cacheKey);
            sc.items.put(cacheKey, value);
        } catch (BeanCreationException e) {
            log.warn(e.getMessage());
            createError = true;
            return;
        }
    }

    public static DataRow get(String[] keys) {
        if (createError)
            return null;
        assert keys.length > 0;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = String.join(".", keys);
            log.debug("get: {}", cacheKey);
            return sc.items.get(cacheKey);
        } catch (BeanCreationException e) {
            log.warn(e.getMessage());
            createError = true;
            return null;
        }

    }

    public static void del(String[] keys) {
        if (createError)
            return;
        assert keys.length > 0;
        try {
            SessionCache sc = Application.getBean(SessionCache.class);
            String cacheKey = String.join(".", keys);
            log.debug("del: {}", cacheKey);
            sc.items.remove(cacheKey);
        } catch (BeanCreationException e) {
            log.warn(e.getMessage());
            createError = true;
            return;
        }
    }

}