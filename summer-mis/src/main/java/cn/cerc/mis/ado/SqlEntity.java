package cn.cerc.mis.ado;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataSetGson;
import cn.cerc.core.EntityKey;
import cn.cerc.core.SqlServer;
import cn.cerc.core.SqlServerType;
import cn.cerc.core.SqlServerTypeException;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.mssql.MssqlDatabase;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.sqlite.SqliteDatabase;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

public class SqlEntity<T> extends SqlQuery implements IHandle {
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
            database.createTable(false);
            buff.put(clazz, database);
        }
        return database;
    }

    public static <U> SqlEntity<U> Create(IHandle handle, Class<U> clazz) {
        ISqlDatabase database = findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        SqlEntity<U> result = new SqlEntity<U>(handle, clazz, sqlServerType);
        result.operator().setTable(database.table());
        result.operator().setOid(database.oid());
        result.add("select * from %s", database.table());
        return result;
    }

    public SqlEntity(IHandle handle, Class<T> clazz, SqlServerType sqlServerType) {
        super(handle, sqlServerType);
        this.clazz = clazz;
    }

    @Override
    public SqlEntity<T> open() {
        super.open();
        this.fields().readDefine(clazz);
        EntityKey ekey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (ekey != null) {
            try (Jedis jedis = JedisFactory.getJedis()) {
                for (DataRow row : this.records())
                    jedis.set(buildCacheKey(ekey, row), row.json());
            }
        }
        return this;
    }

    public String buildCacheKey(EntityKey entityKey, DataRow row) {
        List<String> keys = new ArrayList<>();
        keys.add(clazz.getName());
        if (entityKey.corpNo())
            keys.add(this.getCorpNo());
        for (String field : entityKey.values())
            keys.add(row.getString(field));
        return MemoryBuffer.buildKey(SystemBuffer.Entity.Cache, keys.toArray(new String[keys.size()]));
    }

    public T findById(Object key) {
        ISqlDatabase database = findDatabase(this, clazz);
        this.add("where %s='%s'", database.oid(), key);
        return this.currentEntity();
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

    public SqlEntity<T> insert(T entity) {
        this.append();
        Utils.objectAsRecord(current(), entity);
        return this;
    }

    public T editEntity() {
        edit();
        return current().asEntity(clazz);
    }

    public SqlEntity<T> update(T entity) {
        Utils.objectAsRecord(current(), entity);
        return this;
    }

    @Override
    public String json() {
        return new DataSetGson<SqlEntity<T>>(this).encode();
    }

    @Override
    public SqlEntity<T> setJson(String json) {
        this.clear();
        if (!Utils.isEmpty(json))
            new DataSetGson<SqlEntity<T>>(this).decode(json);
        return this;
    }

}
