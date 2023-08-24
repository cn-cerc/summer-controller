package cn.cerc.mis.ado;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;
import cn.cerc.db.redis.JedisFactory;
import redis.clients.jedis.Jedis;

public class EntityQuery {
    private static final Logger log = LoggerFactory.getLogger(EntityQuery.class);

    public static <T extends EntityImpl> Optional<T> findOne(IHandle handle, Class<T> clazz, String... values) {
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

        ISupplierFindOne<Optional<T>> supplier;
        if (entityKey.smallTable())
            supplier = (values) -> findOneForSmallTable(handle, clazz, null, values);
        else
            supplier = (values) -> findOne(handle, clazz, values);

        return new FindOneBatch<T>(supplier);
    }

    /**
     * @param <T>          entity 类型
     * @param handle       IHandle
     * @param clazz        entity.class
     * @param actionInsert 在找不到时，是否要插入一笔，可为null
     * @param values       查找参数
     * @return 用于小表，取其中一笔数据，若找不到就将整个表数据全载入缓存，下次调用时可直接读取缓存数据，减少sql的开销
     */
    public static <T extends EntityImpl> Optional<T> findOneForSmallTable(IHandle handle, Class<T> clazz,
            Consumer<T> actionInsert, String... values) {
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

        String[] params = new String[values.length - 1];
        for (int i = 0; i < values.length - 1; i++)
            params[i] = values[i];

        SqlQuery query = EntityMany.open(handle, clazz, params).dataSet();
        if (query.size() > SqlText.MAX_RECORDS)
            log.warn("{} -> {} entity size {} larger than {}", handle.getCorpNo(), clazz.getName(), query.size(),
                    SqlText.MAX_RECORDS);
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

        EntityOne<T> loadOne = EntityOne.open(handle, clazz, values);
        if (loadOne.isPresent())
            return Optional.of(loadOne.get());
        if (actionInsert != null)
            loadOne.orElseInsert(actionInsert);
        return Optional.empty();
    }

    public static <T extends EntityImpl> Set<T> findMany(IHandle handle, Class<T> clazz, String... values) {
        return new EntityMany<T>(handle, clazz, SqlWhere.create(handle, clazz, values).build(), true, true).stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static <T extends EntityImpl> Set<T> findMany(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityMany<T>(handle, clazz, sqlText, true, true).stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static <T extends EntityImpl> Set<T> findMany(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityMany<T>(handle, clazz, where.build(), true, true).stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
