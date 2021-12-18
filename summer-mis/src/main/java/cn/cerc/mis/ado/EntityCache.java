package cn.cerc.mis.ado;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.CacheLevelEnum;
import cn.cerc.core.DataRow;
import cn.cerc.core.EntityKey;
import cn.cerc.core.EntityUtils;
import cn.cerc.core.ISession;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

public class EntityCache<T> implements IHandle {
    private static final Logger log = LoggerFactory.getLogger(EntityCache.class);
    public static final int Expire = 3600;
    public static final int MaxRecord = 2000;
    private ISession session;
    private Class<T> clazz;
    private EntityKey entityKey;

    public static <U> EntityCache<U> Create(IHandle handle, Class<U> clazz) {
        return new EntityCache<U>(handle, clazz);
    }

    public EntityCache(IHandle handle, Class<T> clazz) {
        super();
        if(handle != null)
            this.session = handle.getSession();
        this.entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (this.entityKey == null)
            throw new RuntimeException("entityKey not define: " + clazz.getSimpleName());
        this.clazz = clazz;
    }

    /**
     * @param values EntityCache.values 标识字段的值
     * @return 从Session缓存读取，若没有开通，则从Redis读取
     */
    public T getSession(String... values) {
        log.debug("getSession: {}.{}", clazz.getSimpleName(), String.join(".", values));
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return getStorage(values);
        if (entityKey.cache() == CacheLevelEnum.RedisAndSession) {
            String[] keys = this.buildDataKeys(values);
            DataRow row = SessionCache.get(keys);
            if (row != null && row.size() > 0) {
                try {
                    return row.asEntity(clazz);
                } catch (Exception e) {
                    log.error("asEntity {} error: {}", clazz.getSimpleName(), row.json());
                    e.printStackTrace();
                    SessionCache.del(keys);
                }
            }
        }
        return getRedis(values);
    }

    /**
     * @param values EntityCache.values 标识字段的值
     * @return 从Redis读取，若没有找到，则从数据库读取
     */
    public T getRedis(String... values) {
        if (entityKey.cache() != CacheLevelEnum.Redis)
            return getStorage(values);

        log.debug("getRedis: {}.{}", clazz.getSimpleName(), String.join(".", values));
        String[] keys = this.buildDataKeys(values);
        String dataKey = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys);
        try (Jedis jedis = JedisFactory.getJedis()) {
            String json = jedis.get(dataKey);
            if (!Utils.isEmpty(json)) {
                try {
                    DataRow row = new DataRow().setJson(json);
                    return row.asEntity(clazz);
                } catch (Exception e) {
                    log.error("asEntity {} error: {}", clazz.getSimpleName(), json);
                    e.printStackTrace();
                    jedis.del(dataKey);
                }
            }
        }
        return getStorage(values);
    }

    /**
     * @param values EntityCache.values 标识字段的值
     * @return 强制从database中读取，并刷新session缓存与redis缓存
     */
    public T getStorage(String... values) {
        log.debug("getStorage: {}.{}", clazz.getSimpleName(), String.join(".", values));
        T entity = null;
        EntityQuery<T> query = EntityQuery.Create(this, clazz);
        query.sql().clear();
        query.add("select");
        query.add(String.join(",", EntityUtils.getFields(clazz).keySet()));
        query.add("from %s", Utils.findTable(clazz));

        Set<String> prefixs = null;
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            String[] keys1 = buildFilterKeys();
            String prefix = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys1);
            try (Jedis jedis = JedisFactory.getJedis()) {
                prefixs = jedis.keys(prefix);
            }
        }

        // 如果缓存没有保存任何key则重新载入数据
        int diff = entityKey.version() == 0 ? 1 : 2;
        String[] keys2 = this.buildDataKeys(values);
        if (prefixs != null && prefixs.size() == 0) {
            query.add("where %s='%s'", entityKey.values()[0], this.getCorpNo());
            query.open();
            for (DataRow row : query.records()) {
                boolean exists = true;
                for (int i = 0; i < keys2.length - diff; i++) {
                    String value = keys2[i + diff];
                    if (!row.getString(entityKey.values()[i]).equals(value))
                        exists = false;
                }
                if (exists)
                    entity = row.asEntity(clazz);
            }
        } else {
            for (int i = 0; i < keys2.length - diff; i++) {
                query.add(i == 0 ? "where" : "and");
                query.add("%s='%s'", entityKey.values()[i], keys2[i + diff]);
            }
            query.open();
            if (!query.eof())
                entity = query.currentEntity();
        }
        if (entity == null) {
            String dataKey = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys2);
            if (entityKey.cache() != CacheLevelEnum.Disabled) {
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.setex(dataKey, Expire, "");
                }
                if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                    SessionCache.set(keys2, new DataRow());
            }
        }
        return entity;
    }

    private String[] buildFilterKeys() {
        int offset = 1;
        if (entityKey.version() > 0)
            offset++;
        if (entityKey.corpNo())
            offset++;

        String[] keys = new String[offset + 1];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();
        if (entityKey.corpNo())
            keys[offset-1] = this.getCorpNo();
        keys[offset] = "*";
        return keys;
    }

    private String[] buildDataKeys(String... values) {
        if ((values.length + (entityKey.corpNo() ? 1 : 0)) != entityKey.values().length)
            throw new RuntimeException("params size is not match");

        int offset = 1;
        if (entityKey.version() > 0)
            offset++;
        if (entityKey.corpNo())
            offset++;

        String[] keys = new String[offset + values.length];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();
        if (entityKey.corpNo())
            keys[offset-1] = this.getCorpNo();
        for (int i = 0; i < values.length; i++)
            keys[offset + i] = values[i];
        return keys;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

}
