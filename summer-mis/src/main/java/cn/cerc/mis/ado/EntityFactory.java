package cn.cerc.mis.ado;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import org.springframework.context.ApplicationContext;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.EntityHelper;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.Application;
import redis.clients.jedis.Jedis;

public class EntityFactory {
    private static ConcurrentMap<String, Class<? extends AdoTable>> items = new ConcurrentHashMap<>();

    public static <T extends EntityImpl> Optional<T> findOne(IHandle handle, Class<T> clazz, Object... values) {
        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (entityKey == null)
            throw new RuntimeException("entityKey not define: " + clazz.getSimpleName());
        if (entityKey.smallTable())
            return findOneForSmallTable(handle, clazz, null, values);
        else
            return new EntityCache<T>(handle, clazz).get(values);
    }

    public static <T extends EntityImpl> FindOneBatch<T> findOneBatch(IHandle handle, Class<T> clazz) {
        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (entityKey == null)
            throw new RuntimeException("entityKey not define: " + clazz.getSimpleName());

        FindOneSupplierImpl<Optional<T>> supplier;
        if (entityKey.smallTable())
            supplier = (values) -> findOneForSmallTable(handle, clazz, null, values);
        else
            supplier = (values) -> findOne(handle, clazz, values);

        return new FindOneBatch<T>(handle, supplier);
    }

    /**
     * 
     * @param <T>          entity 类型
     * @param handle       IHandle
     * @param clazz        entity.class
     * @param actionInsert 在找不到时，是否要插入一笔，可为null
     * @param values       查找参数
     * @return 用于小表，取其中一笔数据，若找不到就将整个表数据全载入缓存，下次调用时可直接读取缓存数据，减少sql的开销
     */
    public static <T extends EntityImpl> Optional<T> findOneForSmallTable(IHandle handle, Class<T> clazz,
            Consumer<T> actionInsert, Object... values) {
        EntityCache<T> cache = new EntityCache<>(handle, clazz);
        String key = EntityCache.buildKey(cache.buildKeys(values));
        try (Jedis jedis = JedisFactory.getJedis()) {
            String json = jedis.get(key);
            if ("".equals(json) || "{}".equals(json))
                return Optional.empty();
            else if (json != null) {
                try {
                    DataRow row = new DataRow().setJson(json);
                    return Optional.of(row.asEntity(clazz));
                } catch (Exception e) {
                    e.printStackTrace();
                    jedis.del(key);
                }
            }
        }

        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (entityKey == null)
            throw new RuntimeException("entityKey not define: " + clazz.getSimpleName());
        int offset = entityKey.corpNo() ? 1 : 0;
        if (entityKey.fields().length != values.length + offset)
            throw new IllegalArgumentException("values size error");

        Object[] params = new Object[values.length - 1];
        for (int i = 0; i < values.length - 1; i++)
            params[i] = values[i];

        SqlQuery query = EntityFactory.loadAll(handle, clazz, params).dataSet();
        for (DataRow row : query) {
            boolean find = offset == 0 ? true : row.getString(entityKey.fields()[0]).equals(handle.getCorpNo());
            for (int i = offset; i < entityKey.fields().length; i++) {
                String field = entityKey.fields()[i];
                if (!row.getString(field).equals(String.valueOf(values[i - offset])))
                    find = false;
            }
            if (find)
                return Optional.of(row.asEntity(clazz));
        }

        EntityQueryOne<T> loadOne = EntityFactory.loadOne(handle, clazz, values);
        if (loadOne.isPresent())
            return Optional.of(loadOne.get());
        if (actionInsert != null)
            loadOne.orElseInsert(actionInsert);
        return Optional.empty();
    }

    public static <T extends EntityImpl> List<T> findAll(IHandle handle, Class<T> clazz, Object... values) {
        return new EntityQueryAll<T>(handle, clazz, SqlWhere.create(handle, clazz, values).build(), true, true)
                .stream().collect(Collectors.toList());
    }

    public static <T extends EntityImpl> List<T> findAll(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityQueryAll<T>(handle, clazz, sqlText, true, true).stream().collect(Collectors.toList());
    }

    public static <T extends EntityImpl> List<T> findAll(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityQueryAll<T>(handle, clazz, where.build(), true, true).stream().collect(Collectors.toList());
    }

    public static <T extends EntityImpl> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz, Object... values) {
        SqlText sql = SqlWhere.create(handle, clazz, values).build();
        return new EntityQueryOne<T>(handle, clazz, sql, false, false);
    }

    public static <T extends EntityImpl> EntityQueryOne<T> loadOneByUID(IHandle handle, Class<T> clazz, long uid) {
        SqlText sql = SqlWhere.create(clazz).eq("UID_", uid).build();
        return new EntityQueryOne<T>(handle, clazz, sql, false, false);
    }

    public static <T extends EntityImpl> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityQueryOne<T>(handle, clazz, sqlText, false, false);
    }

    public static <T extends EntityImpl> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz,
            Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityQueryOne<T>(handle, clazz, where.build(), false, false);
    }

    public static <T extends EntityImpl> EntityQueryAll<T> loadAll(IHandle handle, Class<T> clazz, Object... values) {
        return new EntityQueryAll<T>(handle, clazz, SqlWhere.create(handle, clazz, values).build(), false, true);
    }

    public static <T extends EntityImpl> EntityQueryAll<T> loadAll(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityQueryAll<T>(handle, clazz, sqlText, false, true);
    }

    public static <T extends EntityImpl> EntityQueryAll<T> loadAll(IHandle handle, Class<T> clazz,
            Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityQueryAll<T>(handle, clazz, where.build(), false, true);
    }

    @Deprecated
    public static <T extends EntityImpl> SqlQuery buildQuery(IHandle handle, Class<T> clazz) {
        EntityHelper<T> helper = EntityHelper.create(clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        SqlQuery query = new SqlQuery(handle, sqlServerType);
        query.operator().setTable(helper.table());
        query.operator().setOid(helper.idFieldCode());
        query.operator().setVersionField(helper.versionFieldCode());
        EntityQuery.registerCacheListener(query, clazz, true);
        return query;
    }

    @Deprecated
    public static <T extends EntityImpl> SqlQuery buildQuery(IHandle handle, Class<T> clazz, SqlText sqlText) {
        SqlQuery query = loadAll(handle, clazz, sqlText).dataSet();
        query.setReadonly(false);
        return query;
    }

    public static Class<? extends AdoTable> searchClass(String table, SqlServerType sqlServerType) {
        ApplicationContext context = Application.getContext();
        if (context == null)
            return null;
        if (items != null)
            return items.get(table);

        synchronized (EntityFactory.class) {
            for (String beanId : context.getBeanNamesForType(AdoTable.class)) {
                Object bean = context.getBean(beanId);
                @SuppressWarnings("unchecked")
                Class<? extends AdoTable> clazz = (Class<? extends AdoTable>) bean.getClass();
                SqlServer server = clazz.getDeclaredAnnotation(SqlServer.class);
                SqlServerType sst = server != null ? server.type() : SqlServerType.Mysql;
                if (sst == sqlServerType) {
                    Entity entity = clazz.getDeclaredAnnotation(Entity.class);
                    if (entity != null && !"".equals(entity.name()))
                        items.put(entity.name(), clazz);
                    else
                        items.put(clazz.getSimpleName(), clazz);
                }
            }
        }

        return items.get(table);
    }

}
