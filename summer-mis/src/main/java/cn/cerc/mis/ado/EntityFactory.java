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

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;
import cn.cerc.mis.core.Application;

public class EntityFactory {
    private static ConcurrentMap<String, Class<? extends AdoTable>> items = new ConcurrentHashMap<>();

    public static <T> Optional<T> findOne(IHandle handle, Class<T> clazz, Object... values) {
        return new EntityCache<T>(handle, clazz).get(values);
    }

    public static <T> List<T> findList(IHandle handle, Class<T> clazz, Object... values) {
        return new EntityQuery<T>(handle, clazz, true).open(SqlWhere.create(handle, clazz, values).build(), true)
                .stream().collect(Collectors.toList());
    }

    public static <T> List<T> findList(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityQuery<T>(handle, clazz, true).open(sqlText, true).stream().collect(Collectors.toList());
    }

    public static <T> List<T> findList(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityQuery<T>(handle, clazz, true).open(where.build(), true).stream().collect(Collectors.toList());
    }

    public static <T> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz, Object... values) {
        SqlText sql = SqlWhere.create(handle, clazz, values).build();
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(sql, false);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQueryOne<T> loadOneByUID(IHandle handle, Class<T> clazz, long uid) {
        SqlText sql = SqlWhere.create(clazz).eq("UID_", uid).build();
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(sql, false);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz, SqlText sqlText) {
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(sqlText, false);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQueryOne<T> loadOne(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(where.build(), false);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQueryList<T> loadList(IHandle handle, Class<T> clazz, Object... values) {
        return new EntityQuery<T>(handle, clazz, true).open(SqlWhere.create(handle, clazz, values).build(), false);
    }

    public static <T> EntityQueryList<T> loadList(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityQuery<T>(handle, clazz, true).open(sqlText, false);
    }

    public static <T> EntityQueryList<T> loadList(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityQuery<T>(handle, clazz, true).open(where.build(), false);
    }

    public static <T> SqlQuery buildQuery(IHandle handle, Class<T> clazz) {
        ISqlDatabase database = EntityQuery.findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        SqlQuery query = new SqlQuery(handle, sqlServerType);
        EntityQuery.registerCacheListener(query, clazz, true);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        return query;
    }

    public static <T> SqlQuery buildQuery(IHandle handle, Class<T> clazz, SqlText sqlText) {
        SqlQuery query = loadList(handle, clazz, sqlText).dataSet();
        query.setReadonly(false);
        return query;
    }

    public static <T> SqlQuery buildQuery(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        SqlQuery query = loadList(handle, clazz, consumer).dataSet();
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
