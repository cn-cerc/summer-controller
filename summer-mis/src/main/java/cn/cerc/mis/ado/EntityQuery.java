package cn.cerc.mis.ado;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.CacheLevelEnum;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlServerTypeException;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;
import cn.cerc.db.mssql.MssqlDatabase;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.sqlite.SqliteDatabase;
import redis.clients.jedis.Jedis;

public class EntityQuery<T> extends Handle {
    private static final Logger log = LoggerFactory.getLogger(EntityQuery.class);
    private static final ConcurrentHashMap<Class<?>, ISqlDatabase> buff = new ConcurrentHashMap<>();
    // 批量写入redis等缓存
    private static final String LUA_SCRIPT_MSETEX = "local keysLen = table.getn(KEYS);local argvLen = table.getn(ARGV);"
            + "local idx=1;local argVIdx=1;for idx=1,keysLen,1 do argVIdx=(idx-1)*2+1; "
            + "redis.call('Set',KEYS[idx],ARGV[argVIdx],'EX',ARGV[argVIdx+1]);end return keysLen;";
    private final SqlQuery query;
    private final Class<T> clazz;
    // 标识为Id的字段
    private Field idFieldDefine = null;
    private String idFieldCode = null;

    public static <T> EntityQuery<T> findOne(IHandle handle, Class<T> clazz, Object... values) {
        SqlText sql = SqlWhere.create(handle, clazz, values).build();
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(sql);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQuery<T> findUid(IHandle handle, Class<T> clazz, long uid) {
        SqlText sql = SqlWhere.create(clazz).eq("UID_", uid).build();
        EntityQuery<T> result = new EntityQuery<T>(handle, clazz, false).open(sql);
        if (result.size() > 1)
            throw new RuntimeException("There're too many records.");
        return result;
    }

    public static <T> EntityQuery<T> findAll(IHandle handle, Class<T> clazz, SqlText sql) {
        return new EntityQuery<T>(handle, clazz, true).open(sql);
    }

    public static SqlQuery buildQuery(IHandle handle, Class<?> clazz) {
        ISqlDatabase database = EntityQuery.findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        SqlQuery query = new SqlQuery(handle, sqlServerType);
        EntityQuery.registerCacheListener(query, clazz, true);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        return query;
    }

    public static ISqlDatabase findDatabase(IHandle handle, Class<?> clazz) {
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

    // 注册与写入缓存相关的事件
    public static <T> void registerCacheListener(SqlQuery target, Class<T> clazz, boolean writeCacheAtOpen) {
        // 在open时，读入字段定义
        target.onAfterOpen(self -> self.fields().readDefine(clazz));
        EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
        if (entityKey == null || entityKey.cache() == CacheLevelEnum.Disabled)
            return;

        // 在open时，写入redis等缓存
        if (writeCacheAtOpen) {
            target.onAfterOpen(query -> {
                int count = 0;
                EntityCache<T> ec1 = EntityCache.Create(query, clazz);
                List<String> batchKeys = new ArrayList<>();
                List<String> batchValues = new ArrayList<>();
                for (DataRow row : query.records()) {
                    if (++count > EntityCache.MaxRecord)
                        break;
                    Object[] keys = ec1.buildKeys(row);
                    batchKeys.add(EntityCache.buildKey(keys));
                    batchValues.add(row.json());
                    batchValues.add("" + entityKey.expire());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.evalsha(jedis.scriptLoad(LUA_SCRIPT_MSETEX), batchKeys, batchValues);
                }
            });
        }

        // 在post(insert、update)时，写入redis等缓存
        target.onAfterPost(row -> {
            EntityCache<T> ec2 = EntityCache.Create(target, clazz);
            Object[] keys = ec2.buildKeys(row);
            try (Jedis jedis = JedisFactory.getJedis()) {
                jedis.setex(EntityCache.buildKey(keys), entityKey.expire(), row.json());
                if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                    SessionCache.set(keys, row);
            }
        });

        // 在delete时，清除redis等缓存
        target.onAfterDelete(row -> {
            EntityCache<T> ec3 = EntityCache.Create(target, clazz);
            Object[] keys = ec3.buildKeys(row);
            try (Jedis jedis = JedisFactory.getJedis()) {
                jedis.del(EntityCache.buildKey(keys));
                if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                    SessionCache.del(keys);
            }
        });
    }

    public EntityQuery(IHandle handle, Class<T> clazz, boolean writeCacheAtOpen) {
        super(handle);
        this.clazz = clazz;
        ISqlDatabase database = findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        query = new SqlQuery(this, server != null ? server.type() : SqlServerType.Mysql);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        registerCacheListener(query, clazz, writeCacheAtOpen);
    }

    public EntityQuery<T> open(SqlText sql) {
        query.setSql(sql);
        query.open();
        return this;
    }

    public T newEntity() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(T entity) {
        query.append();
        if (entity instanceof AdoTable)
            ((AdoTable) entity).insertTimestamp(query);
        query.current().loadFromEntity(entity);
        query.post();
    }

    public void save(T entity) {
        if (isNewRecord(entity)) {
            query.append();
            if (entity instanceof AdoTable)
                ((AdoTable) entity).insertTimestamp(query);
        } else {
            query.edit();
            if (entity instanceof AdoTable)
                ((AdoTable) entity).updateTimestamp(query);
        }
        query.current().loadFromEntity(entity);
        query.post();
    }

    public boolean deleteAll() {
        if (query.eof())
            return false;
        query.first();
        while (!query.eof())
            query.delete();
        return true;
    }

    public boolean deleteIf(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        if (query.eof())
            return false;
        query.first();
        while (!query.eof()) {
            T entity = this.query.current().asEntity(clazz);
            if (predicate.test(entity))
                query.delete();
            else
                query.next();
        }
        return true;
    }

    public Optional<T> updateAll(Consumer<T> action) {
        Objects.requireNonNull(action);
        T entity = null;
        for (int i = 0; i < query.size(); i++) {
            DataRow row = query.records().get(i);
            entity = row.asEntity(this.clazz);
            action.accept(entity);
            save(entity);
        }
        return Optional.ofNullable(entity);
    }

    public Optional<T> updateIf(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        T entity = null;
        for (int i = 0; i < query.size(); i++) {
            DataRow row = query.records().get(i);
            entity = row.asEntity(this.clazz);
            if (predicate.test(entity))
                save(entity);
        }
        return Optional.ofNullable(entity);
    }

    public int size() {
        return query.size();
    }

    public SqlQuery dataSet() {
        return query;
    }

    public Optional<T> get() {
        return this.stream().findFirst();
    }

    public Stream<T> stream() {
        return query.records().stream().map(item -> item.asEntity(clazz));
    }

    /**
     * @param entity Entity实体对象
     * @return 判断传入的entity对象，在当前记录集中是不是新的
     */
    private boolean isNewRecord(T entity) {
        DataRow row = query.current();
        if (row == null)
            return true;

        if (idFieldDefine == null) {
            Map<String, Field> items = DataRow.getEntityFields(clazz);
            for (String fieldCode : items.keySet()) {
                Field field = items.get(fieldCode);
                Id id = field.getAnnotation(Id.class);
                if (id != null) {
                    idFieldDefine = field;
                    idFieldCode = fieldCode;
                    break;
                }
            }
        }

        if (idFieldDefine == null)
            throw new IllegalArgumentException("id define not exists");

        Object idValue = null;
        try {
            idValue = idFieldDefine.get(entity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        if (idValue == null)
            return true;

        if (row.getValue(idFieldCode).equals(idValue))
            return false;
        else {
            log.warn("id 字段被变更，由修改记录变成了增加记录，请检查！");
            return true;
        }
    }
}
