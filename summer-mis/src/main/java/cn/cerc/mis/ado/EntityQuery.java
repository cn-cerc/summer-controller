package cn.cerc.mis.ado;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.CacheLevelEnum;
import cn.cerc.core.DataRow;
import cn.cerc.core.DataSetGson;
import cn.cerc.core.EntityKey;
import cn.cerc.core.SqlServer;
import cn.cerc.core.SqlServerType;
import cn.cerc.core.SqlServerTypeException;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.mssql.MssqlDatabase;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.sqlite.SqliteDatabase;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
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
            if (ServerConfig.isServerDevelop())
                database.createTable(false);
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
        EntityKey ekey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (ekey != null && ekey.cache() != CacheLevelEnum.Disabled) {
            if (this.size() <= EntityCache.MaxRecord) {
                try (Jedis jedis = JedisFactory.getJedis()) {
                    for (DataRow row : this.records()) {
                        String[] keys = buildRowKeys(ekey, row);
                        log.debug("set: {}", String.join(".", keys));
                        String dataKey = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys);
                        jedis.setex(dataKey, EntityCache.Expire, row.json());
                        if (ekey.cache() == CacheLevelEnum.RedisAndSession)
                            SessionCache.set(keys, row);
                    }
                }
            }
            this.onAfterPost(row -> {
                try (Jedis jedis = JedisFactory.getJedis()) {
                    String[] keys = buildRowKeys(ekey, row);
                    log.debug("set: {}", String.join(".", keys));
                    String dataKey = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys);
                    jedis.setex(dataKey, EntityCache.Expire, row.json());
                    if (ekey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
            });
            this.onAfterDelete(row -> {
                try (Jedis jedis = JedisFactory.getJedis()) {
                    String[] keys = buildRowKeys(ekey, row);
                    log.debug("del: {}", String.join(".", keys));
                    String dataKey = MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys);
                    jedis.del(dataKey);
                    if (ekey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.del(keys);
                }
            });
        }
        return this;
    }

    public String[] buildRowKeys(EntityKey entityKey, DataRow row) {
        int offset = 1;
        if (entityKey.version() > 0)
            offset++;

        String[] keys = new String[offset + entityKey.values().length];
        keys[0] = clazz.getSimpleName();
        if (entityKey.version() > 0)
            keys[1] = "" + entityKey.version();
        
        for (int i = 0; i < entityKey.values().length; i++)
            keys[offset + i] = row.getString(entityKey.values()[i]);
        return keys;
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
        Utils.objectAsRecord(current(), entity);
        return this;
    }

    public T editEntity() {
        edit();
        return current().asEntity(clazz);
    }

    public EntityQuery<T> update(T entity) {
        Utils.objectAsRecord(current(), entity);
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

}
