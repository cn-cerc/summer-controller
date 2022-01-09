package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.CacheLevelEnum;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.FieldDefs;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.SystemBuffer;
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
    public Optional<T> get(Object... values) {
        log.debug("getSession: {}.{}", clazz.getSimpleName(), joinToKey(values));
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return getStorage(values);
        if (entityKey.cache() == CacheLevelEnum.RedisAndSession) {
            Object[] keys = this.buildKeys(values);
            DataRow row = SessionCache.get(keys);
            if (row != null && row.size() > 0) {
                try {
                    return Optional.of(row.asEntity(clazz));
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
    public Optional<T> getRedis(Object... values) {
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            log.debug("getRedis: {}.{}", clazz.getSimpleName(), joinToKey(values));
            Object[] keys = this.buildKeys(values);
            try (Jedis jedis = JedisFactory.getJedis()) {
                String json = jedis.get(EntityCache.buildKey(keys));
                if ("".equals(json) || "{}".equals(json))
                    return Optional.empty();
                else if (json != null) {
                    try {
                        DataRow row = new DataRow().setJson(json);
                        if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                            SessionCache.set(keys, row);
                        return Optional.of(row.asEntity(clazz));
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
    public Optional<T> getStorage(Object... values) {
        log.debug("getStorage: {}.{}", clazz.getSimpleName(), joinToKey(values));
        T entity = null;
        if (entityKey.virtual()) {
            entity = getVirtualEntity(values);
        } else {
            entity = getTableEntity(values);
        }
        if (entity == null && entityKey.cache() != CacheLevelEnum.Disabled) {
            Object[] keys = this.buildKeys(values);
            try (Jedis jedis = JedisFactory.getJedis()) {
                if (this.clazz.getSimpleName().equals("PartinfoEntity"))
                    throw new RuntimeException("找不到商品料号: " + values[0]);
                jedis.setex(buildKey(keys), entityKey.expire(), "");
            }
            if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                SessionCache.set(keys, new DataRow());
        }
        return Optional.ofNullable(entity);
    }

    protected T getVirtualEntity(Object... values) {
        int diff = entityKey.version() == 0 ? 1 : 2;
        Object[] keys = this.buildKeys(values);
        // 尝试直接对entity进行填充
        DataRow headIn = new DataRow(new FieldDefs(clazz));
        for (int i = 0; i < keys.length - diff; i++)
            headIn.setValue(entityKey.fields()[i], keys[i + diff]);
        //
        T obj = newVirtualEntity();
        VirtualEntityImpl impl = (VirtualEntityImpl) obj;
        headIn.saveToEntity(obj);
        if (impl.fillItem(this, obj, headIn)) {
            DataRow row = new DataRow();
            row.loadFromEntity(obj);
            try (Jedis jedis = JedisFactory.getJedis()) {
                jedis.setex(buildKey(keys), entityKey.expire(), row.json());
            }
            if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                SessionCache.set(keys, row);
            return obj;
        }

        // 载入全部的Entity
        DataSet query = impl.loadItems(this, headIn);
        if (query == null || query.size() == 0)
            return null;

        // 存入缓存
        if (entityKey.cache() != CacheLevelEnum.Disabled) {
            try (Jedis jedis = JedisFactory.getJedis()) {
                for (DataRow row : query) {
                    Object[] rowKeys = buildKeys(row);
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
                Object value = keys[i + diff];
                if (!row.getValue(entityKey.fields()[i]).equals(value))
                    exists = false;
            }
            if (exists)
                return row.asEntity(clazz);
        }
        return null;
    }

    private T getTableEntity(Object... values) {
        T entity = null;
        int diff = entityKey.version() == 0 ? 1 : 2;
        // 如果缓存没有保存任何key则重新载入数据
        Object[] keys = this.buildKeys(values);
        if (listKeys() == null && entityKey.corpNo()) {
            SqlText sql = SqlWhere.create(this, clazz).build();
            SqlQuery query = EntityQuery.create(this, clazz);
            query.setSql(sql);
            query.open();
            for (DataRow row : query) {
                boolean exists = true;
                for (int i = 0; i < keys.length - diff; i++) {
                    Object value = keys[i + diff];
                    if (!value.equals(row.getValue(entityKey.fields()[i])))
                        exists = false;
                }
                if (exists)
                    entity = row.asEntity(clazz);
            }
        } else {
            SqlText sql = SqlWhere.create(this, clazz, values).build();
            EntityQuery<T> query = EntityQuery.findAll(this, clazz, sql);
            if (query.size() > 1)
                throw new RuntimeException("There're too many records.");
            entity = query.get().orElse(null);
        }
        return entity;
    }

    public void del(Object... values) {
        if (entityKey.cache() == CacheLevelEnum.Disabled)
            return;
        Object[] keys = this.buildKeys(values);
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

        Object[] keys = new Object[offset + 1];
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

    public static String buildKey(Object... keys) {
        int flag = SystemBuffer.Entity.Cache.getStartingPoint() + SystemBuffer.Entity.Cache.ordinal();
        return flag + "." + joinToKey(keys);
    }

    public static String joinToKey(Object... keys) {
        StringBuffer sb = new StringBuffer();
        int count = 0;
        for (Object key : keys) {
            if (++count > 1)
                sb.append(".");
            if (key instanceof Boolean)
                sb.append((Boolean) key ? 1 : 0);
            else if (key != null)
                sb.append(key);
        }
        return sb.toString();
    }

    public Object[] buildKeys(Object... values) {
        if ((values.length + (entityKey.corpNo() ? 1 : 0)) != entityKey.fields().length)
            throw new RuntimeException("params size is not match");

        int offset = 1;
        if (entityKey.version() > 0)
            offset++;
        if (entityKey.corpNo())
            offset++;

        Object[] keys = new Object[offset + values.length];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();
        if (entityKey.corpNo())
            keys[offset - 1] = this.getCorpNo();
        for (int i = 0; i < values.length; i++)
            keys[offset + i] = values[i];
        return keys;
    }

    public Object[] buildKeys(DataRow row) {
        int offset = 1;
        if (entityKey.version() > 0)
            offset++;

        Object[] keys = new Object[offset + entityKey.fields().length];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();

        for (int i = 0; i < entityKey.fields().length; i++)
            keys[offset + i] = row.getValue(entityKey.fields()[i]);
        return keys;
    }

    public interface VirtualEntityImpl {
        /**
         * @param handle IHandle
         * @param entity Entity Object
         * @param headIn 要赋值的内容
         * @return 直接给 entity 赋值 values 是否成功
         */
        boolean fillItem(IHandle handle, Object entity, DataRow headIn);

        /**
         * 先调用fillEntity，在其返回false时，再调用此函数
         * 
         * @param handle IHandle
         * @param headIn headIn 标识字段的值
         * @return 返回载入的数据，允许返回null
         */
        DataSet loadItems(IHandle handle, DataRow headIn);
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

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    public static <T> Optional<T> find(IHandle handle, Class<T> clazz, Object... keys) {
        EntityCache<T> cache = new EntityCache<T>(handle, clazz);
        return cache.get(keys);
    }
}
