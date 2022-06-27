package cn.cerc.mis.custom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.Handle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Utils;
import cn.cerc.db.mysql.MysqlQuery;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.cache.ISessionCache;
import cn.cerc.mis.core.IAppLanguage;
import cn.cerc.mis.core.ISystemTable;
import redis.clients.jedis.Jedis;

@Component
public class AppLanguageDefault implements IAppLanguage, ISessionCache {
    private static final Logger log = LoggerFactory.getLogger(AppLanguageDefault.class);
    private static final String CACHE_KEY = "UserOptions";
    @Autowired
    private ISystemTable systemTable;
    // 存储每个用户的设置值
    private Map<String, String> items;;

    @Override
    public String getLanguageId(ISession session, String defaultValue) {
        String result = defaultValue;
        String userCode = session.getUserCode();
        if (Utils.isEmpty(userCode))
            return result;

        if (items == null) {
            try (Jedis redis = JedisFactory.getJedis()) {
                items = redis.hgetAll(CACHE_KEY);
            }
        }
        if (items.containsKey(userCode))
            return items.get(userCode);

        synchronized (this) {
            try {
                MysqlQuery ds = new MysqlQuery(new Handle(session));
                ds.add("select Value_ from %s", systemTable.getUserOptions());
                ds.add("where Code_='%s' and UserCode_='%s'", "Lang_", userCode);
                ds.open();
                if (!ds.eof()) {
                    result = ds.getString("Value_");
                }
                items.put(userCode, result);
                try (Jedis redis = JedisFactory.getJedis()) {
                    redis.hset(CACHE_KEY, userCode, result);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void clearCache() {
        if (this.items != null)
            this.items.clear();
        try (Jedis redis = JedisFactory.getJedis()) {
            redis.del(CACHE_KEY);
        }
    }

}
