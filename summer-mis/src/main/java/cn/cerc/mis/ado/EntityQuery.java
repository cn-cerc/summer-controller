package cn.cerc.mis.ado;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.CacheLevelEnum;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSetGson;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlServerTypeException;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.Utils;
import cn.cerc.db.mssql.MssqlDatabase;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.sqlite.SqliteDatabase;
import redis.clients.jedis.Jedis;

public class EntityQuery<T> extends SqlQuery implements IHandle {
    private static final Logger log = LoggerFactory.getLogger(EntityQuery.class);
    private static final long serialVersionUID = 8276125658457479833L;
    private static ConcurrentHashMap<Class<?>, ISqlDatabase> buff = new ConcurrentHashMap<>();
    private Class<T> clazz;

    public interface InitializationTableImpl {
        void initialization(IHandle handle);
    }

    private static ISqlDatabase findDatabase(IHandle handle, Class<?> clazz) {
        ISqlDatabase database = buff.get(clazz);
        if (database == null) {
            SqlServer server = clazz.getAnnotation(SqlServer.class);
            SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
            if (sqlServerType == SqlServerType.Mysql)
                database = new MysqlDatabase(handle, clazz);
            else if (sqlServerType == SqlServerType.Mssql)
                database = new MssqlDatabase(handle, clazz);
            else if (sqlServerType == SqlServerType.Sqlite)
                database = new SqliteDatabase(handle, clazz);
            else
                throw new SqlServerTypeException();
//            if (ServerConfig.isServerDevelop()) {
//                EntityKey ekey = clazz.getDeclaredAnnotation(EntityKey.class);
//                if (ekey == null || !ekey.virtual())
//                    database.createTable(false);
//            }
            buff.put(clazz, database);
        }
        return database;
    }

    public static <U> EntityQuery<U> Create(IHandle handle, Class<U> clazz) {
        ISqlDatabase database = findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        EntityQuery<U> query = new EntityQuery<U>(handle, clazz, sqlServerType);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        return query;
    }

    public EntityQuery(IHandle handle, Class<T> clazz, SqlServerType sqlServerType) {
        super(handle, sqlServerType);
        this.clazz = clazz;
    }

    @Override
    public EntityQuery<T> open() {
        super.open();
        this.fields().readDefine(clazz);
        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (entityKey != null && entityKey.cache() != CacheLevelEnum.Disabled) {
            EntityCache<T> ec1 = EntityCache.Create(this, clazz);
            try (Jedis jedis = JedisFactory.getJedis()) {
                int count = 0;
                for (DataRow row : this.records()) {
                    if (++count > EntityCache.MaxRecord)
                        break;
                    Object[] keys = ec1.buildKeys(row);
                    log.debug("set: {}", EntityCache.joinToKey(keys));
                    jedis.setex(EntityCache.buildKey(keys), entityKey.expire(), row.json());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
            }
            this.onAfterPost(row -> {
                EntityCache<T> ec2 = EntityCache.Create(this, clazz);
                Object[] keys = ec2.buildKeys(row);
                log.debug("set: {}", EntityCache.joinToKey(keys));
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.setex(EntityCache.buildKey(keys), entityKey.expire(), row.json());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
            });
            this.onAfterDelete(row -> {
                EntityCache<T> ec3 = EntityCache.Create(this, clazz);
                Object[] keys = ec3.buildKeys(row);
                log.debug("del: {}", EntityCache.joinToKey(keys));
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.del(EntityCache.buildKey(keys));
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.del(keys);
                }
            });
        }
        return this;
    }

    public T currentEntity() {
        DataRow row = current();
        if (row == null)
            return null;
        return row.asEntity(clazz);
    }

    public T newEntity() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public EntityQuery<T> insert(T entity) {
        this.append();
        current().loadFromEntity(entity);
        return this;
    }

    public T editEntity() {
        edit();
        return current().asEntity(clazz);
    }

    public EntityQuery<T> update(T entity) {
        current().loadFromEntity(entity);
        return this;
    }

    @Override
    public String json() {
        return new DataSetGson<EntityQuery<T>>(this).encode();
    }

    @Override
    public EntityQuery<T> setJson(String json) {
        this.clear();
        if (!Utils.isEmpty(json))
            new DataSetGson<EntityQuery<T>>(this).decode(json);
        return this;
    }

    public String table() {
        return Utils.findTable(clazz);
    }

    @Override
    @Deprecated
    public int attach(String sqlText) {
        return super.attach(sqlText);
    }

    public EntityQuery<T> openByKeys(Object... values) {
        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        int offset = entityKey.corpNo() ? 1 : 0;
        if ((values.length + offset) > entityKey.fields().length)
            throw new RuntimeException("params size is not match");

        this.clear();
        SqlText sql = this.sql();
        sql.add("select * from %s", Utils.findTable(clazz));
        if (entityKey.corpNo())
            sql.add("where %s='%s'", entityKey.fields()[0], this.getCorpNo());
        for (int i = 0; i < values.length; i++) {
            String field = entityKey.fields()[i + offset];
            Object value = values[i];
            sql.add((i + offset) == 0 ? "where" : "and");
            if (value instanceof Boolean) {
                sql.add("%s=%d", field, (Boolean) value ? 1 : 0);
            } else if (value != null)
                sql.add("%s='%s'", field, Utils.safeString(String.valueOf(value)));
            else
                sql.add("(%s is null)", field);
        }
        this.open();
        return this;
    }

}
