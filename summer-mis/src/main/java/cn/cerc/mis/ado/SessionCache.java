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

    public static void set(String[] keys, DataRow value) {
        log.debug("set: {}", String.join(".", keys));
        assert keys.length > 0;
        assert value != null;
        SessionCache sc = Application.getBean(SessionCache.class);
        sc.items.put(String.join(".", keys), value);
    }

    public static DataRow get(String[] keys) {
        log.debug("get: {}", String.join(".", keys));
        assert keys.length > 0;
        SessionCache sc = Application.getBean(SessionCache.class);
        return sc.items.get(String.join(".", keys));
    }
    
    public static void del(String[] keys) {
        log.debug("del: {}", String.join(".", keys));
        assert keys.length > 0;
        SessionCache sc = Application.getBean(SessionCache.class);
        sc.items.remove(String.join(".", keys));    
    }
    
}
