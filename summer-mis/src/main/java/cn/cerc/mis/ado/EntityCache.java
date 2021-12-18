package cn.cerc.mis.ado;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.CacheLevelEnum;
import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
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
    public static final int MaxRecord = 2000;
    private ISession session;
    private Class<T> clazz;
    private EntityKey entityKey;

    public static <U> EntityCache<U> Create(IHandle handle, Class<U> clazz) {
        return new EntityCache<U>(handle, clazz);
    }

    public EntityCache(IHandle handle, Class<T> clazz) {
        super();
        if (handle != null)
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
    public T get(String... values) {
        log.debug("getSession: {}.{}", clazz.getSimpleName(), String.join(".", values));
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return getStorage(values);
        if (entityKey.cache() == CacheLevelEnum.RedisAndSession) {
            String[] keys = this.buildKeys(values);
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
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            log.debug("getRedis: {}.{}", clazz.getSimpleName(), String.join(".", values));
            String[] keys = this.buildKeys(values);
            try (Jedis jedis = JedisFactory.getJedis()) {
                String json = jedis.get(EntityCache.buildKey(keys));
                if ("".equals(json))
                    return null;
                else if (json != null) {
                    try {
                        DataRow row = new DataRow().setJson(json);
                        return row.asEntity(clazz);
                    } catch (Exception e) {
                        log.error("asEntity {} error: {}", clazz.getSimpleName(), json);
                        e.printStackTrace();
                        jedis.del(EntityCache.buildKey(keys));
                        if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                            SessionCache.del(keys);
                    }
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
        if (entityKey.virtual()) {
            entity = getVirtualEntity();
        } else {
            entity = getTableEntity();
        }
        if (entity == null)
            setEmpty(values);
        return entity;
    }

    private T getTableEntity(String... values) {
        T entity = null;
        int diff = entityKey.version() == 0 ? 1 : 2;
        EntityQuery<T> query = EntityQuery.Create(this, clazz);
        query.sql().clear();
        query.add("select").add(String.join(",", EntityUtils.getFields(clazz).keySet()));
        query.add("from %s", Utils.findTable(clazz));
        // 如果缓存没有保存任何key则重新载入数据
        String[] keys = this.buildKeys(values);
        if (listKeys() != null) {
            query.add("where %s='%s'", entityKey.fields()[0], this.getCorpNo());
            query.open();
            for (DataRow row : query.records()) {
                boolean exists = true;
                for (int i = 0; i < keys.length - diff; i++) {
                    String value = keys[i + diff];
                    if (!row.getString(entityKey.fields()[i]).equals(value))
                        exists = false;
                }
                if (exists)
                    entity = row.asEntity(clazz);
            }
        } else {
            for (int i = 0; i < keys.length - diff; i++) {
                query.add(i == 0 ? "where" : "and");
                query.add("%s='%s'", entityKey.fields()[i], keys[i + diff]);
            }
            query.open();
            if (query.size() == 1)
                entity = query.currentEntity();
            else if (query.size() > 1)
                throw new RuntimeException("error: size > 1");
        }
        return entity;
    }

    private T getVirtualEntity(String... values) {
        int diff = entityKey.version() == 0 ? 1 : 2;
        String[] keys = this.buildKeys(values);
        T obj = newVirtualEntity();
        VirtualEntityImpl impl = (VirtualEntityImpl) obj;

        // 尝试直接对entity进行填充
        DataRow headIn = new DataRow();
        for (int i = 0; i < keys.length - diff; i++)
            headIn.setValue(entityKey.fields()[i], keys[i + diff]);
        if (impl.fillItem(this, obj, headIn)) {
            DataRow row = new DataRow();
            Utils.objectAsRecord(row, obj);
            try (Jedis jedis = JedisFactory.getJedis()) {
                jedis.setex(buildKey(keys), entityKey.expire(), row.json());
            }
            if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                SessionCache.set(keys, row);
            return obj;
        }

        // 载入全部的Entity
        DataSet query = impl.loadItems(this, values);
        if (query == null || query.size() == 0)
            return null;

        // 存入缓存
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            try (Jedis jedis = JedisFactory.getJedis()) {
                for (DataRow row : query) {
                    String[] rowKeys = buildKeys(row);
                    jedis.setex(buildKey(rowKeys), entityKey.expire(), row.json());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(rowKeys, row);
                }
            }
        }

        // 查找返回值中是否有符合的entity
        for (DataRow row : query) {
            boolean exists = true;
            for (int i = 0; i < keys.length - diff; i++) {
                String value = keys[i + diff];
                if (!row.getString(entityKey.fields()[i]).equals(value))
                    exists = false;
            }
            if (exists)
                return row.asEntity(clazz);
        }
        return null;
    }

    public void del(String... values) {
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return;
        String[] keys = this.buildKeys(values);
        try (Jedis jedis = JedisFactory.getJedis()) {
            jedis.del(buildKey(keys));
        }
        if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
            SessionCache.del(keys);
    }

    /**
     * @return 返回已缓存的key*列表，如果列表数量为0，则返回null
     */
    public Set<String> listKeys() {
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return null;

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
            keys[offset - 1] = this.getCorpNo();
        keys[offset] = "*";

        try (Jedis jedis = JedisFactory.getJedis()) {
            Set<String> items = jedis.keys(EntityCache.buildKey(keys));
            return items.size() > 0 ? items : null;
        }
    }

    public static String buildKey(String[] keys) {
        return MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys);
    }

    public String[] buildKeys(String... values) {
        if ((values.length + (entityKey.corpNo() ? 1 : 0)) != entityKey.fields().length)
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
            keys[offset - 1] = this.getCorpNo();
        for (int i = 0; i < values.length; i++)
            keys[offset + i] = values[i];
        return keys;
    }

    public String[] buildKeys(DataRow row) {
        int offset = 1;
        if (entityKey.version() > 0)
            offset++;

        String[] keys = new String[offset + entityKey.fields().length];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();

        for (int i = 0; i < entityKey.fields().length; i++)
            keys[offset + i] = row.getString(entityKey.fields()[i]);
        return keys;
    }

    public interface VirtualEntityImpl {
        /**
         * @param handle IHandle
         * @param entity Entity Object
         * @param values 要赋值的内容
         * @return 直接给 entity 赋值 values 是否成功
         */
        boolean fillItem(IHandle handle, Object entity, DataRow headIn);

        /**
         * 先调用fillItem，在其返回false时，再调用此函数
         * 
         * @param handle
         * @param values
         * @return 返回载入的数据，允许返回null
         */
        DataSet loadItems(IHandle handle, String... values);
    }

    private T newVirtualEntity() {
        T obj;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!(obj instanceof VirtualEntityImpl))
            throw new RuntimeException(clazz.getSimpleName() + " not support VirtualEntityImpl");
        return obj;
    }

    private void setEmpty(String... values) {
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            String[] keys = this.buildKeys(values);
            try (Jedis jedis = JedisFactory.getJedis()) {
                jedis.setex(buildKey(keys), entityKey.expire(), "");
            }
            if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                SessionCache.set(keys, new DataRow());
        }
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
