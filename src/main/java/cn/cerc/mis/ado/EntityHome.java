package cn.cerc.mis.ado;

import java.lang.reflect.Field;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.CacheLevelEnum;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.EntityHelper;
import cn.cerc.db.core.EntityHomeImpl;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.HistoryLoggerImpl;
import cn.cerc.db.core.HistoryTypeEnum;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlServerTypeException;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.mssql.MssqlDatabase;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.redis.Redis;
import cn.cerc.db.sqlite.SqliteDatabase;
import cn.cerc.db.testsql.TestsqlServer;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.log.JayunLogParser;
import redis.clients.jedis.Jedis;

public abstract class EntityHome<T extends EntityImpl> extends Handle implements EntityHomeImpl {
    private static final Logger log = LoggerFactory.getLogger(EntityHome.class);
    private static final ConcurrentHashMap<Class<?>, ISqlDatabase> buff = new ConcurrentHashMap<>();

    // 构建 Lua 脚本，批量写入 redis 缓存
    public static final String luaScript = """
            for i, key in pairs(KEYS) do
                redis.call('SET', key, ARGV[i * 2 - 1], 'EX', ARGV[i * 2])
            end
            return #KEYS
            """;
    protected final SqlQuery query;
    protected final Class<T> clazz;
    protected EntityHelper<T> helper;

    public static ISqlDatabase findDatabase(IHandle handle, Class<? extends EntityImpl> clazz) {
        ISqlDatabase database = buff.get(clazz);
        if (database == null) {
            SqlServer server = EntityHelper.get(clazz).sqlServer();
            SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
            if (TestsqlServer.enabled())
                sqlServerType = SqlServerType.Testsql;
            if (sqlServerType == SqlServerType.Mysql)
                database = new MysqlDatabase(handle, clazz);
            else if (sqlServerType == SqlServerType.Mssql)
                database = new MssqlDatabase(handle, clazz);
            else if (sqlServerType == SqlServerType.Sqlite)
                database = new SqliteDatabase(handle, clazz);
            else
                throw new SqlServerTypeException();
            buff.put(clazz, database);
        }
        return database;
    }

    // 注册与写入缓存相关的事件
    public static <T extends EntityImpl> void registerCacheListener(SqlQuery target, Class<T> clazz,
            boolean writeCacheAtOpen) {
        // 在open时，读入字段定义
        target.onAfterOpen(self -> self.fields().readDefine(clazz));
        EntityKey entityKey = EntityHelper.get(clazz).entityKey();
        if (entityKey == null || entityKey.cache() == CacheLevelEnum.Disabled)
            return;

        // 在open时，写入redis等缓存
        if (writeCacheAtOpen) {
            target.onAfterOpen(query -> {
                int count = 0;
                EntityCache<T> ec1 = new EntityCache<T>(query, clazz);
                List<String> batchKeys = new ArrayList<>();
                List<String> batchValues = new ArrayList<>();
                for (DataRow row : query.records()) {
                    if (++count > EntityCache.MaxRecord)
                        break;
                    String[] keys = ec1.buildKeys(row);
                    batchKeys.add(EntityCache.buildKey(keys));
                    batchValues.add(row.json());
                    batchValues.add("" + entityKey.expire());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
                try (Redis jedis = new Redis()) {
                    String sha = jedis.scriptLoad(luaScript);
                    jedis.evalsha(sha, batchKeys, batchValues);
                }
            });
        }

        // 在post(insert、update)时，写入redis等缓存
        target.onAfterPost(row -> {
            for (var class1 : EntityHome.getFamily(clazz)) {
                EntityCache<?> ec2 = new EntityCache<>(target, class1);
                String[] keys = ec2.buildKeys(row);
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.setex(EntityCache.buildKey(keys), entityKey.expire(), row.json());
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.set(keys, row);
                }
            }
        });

        // 在delete时，清除redis等缓存
        target.onAfterDelete(row -> {
            for (var class1 : EntityHome.getFamily(clazz)) {
                EntityCache<?> ec3 = new EntityCache<>(target, class1);
                String[] keys = ec3.buildKeys(row);
                try (Jedis jedis = JedisFactory.getJedis()) {
                    jedis.del(EntityCache.buildKey(keys));
                    if (entityKey.cache() == CacheLevelEnum.RedisAndSession)
                        SessionCache.del(keys);
                }
            }
        });
    }

    public EntityHome(IHandle handle, Class<T> clazz, SqlText sql, boolean useSlaveServer, boolean writeCacheAtOpen) {
        super(handle);
        this.clazz = clazz;
        this.helper = EntityHelper.get(clazz);
        query = new SqlQuery(this, helper.sqlServerType());
        query.operator().setTable(helper.tableName());
        query.operator().setOid(helper.idFieldCode());
        query.operator().setVersionField(helper.versionFieldCode());
        registerCacheListener(query, clazz, writeCacheAtOpen);
        if (sql != null) {
            query.setSql(sql);
            try {
                if (useSlaveServer)
                    query.openReadonly();
                else
                    query.open();
            } catch (RuntimeException e) {
                if (!(e.getCause() instanceof SQLSyntaxErrorException))
                    throw e;

                if (helper.sqlServerType() != SqlServerType.Mysql)
                    throw e;

                var msg = ((SQLSyntaxErrorException) e.getCause()).getMessage();
                if (!msg.startsWith("Table ") || !msg.endsWith(" doesn't exist"))
                    throw e;

                log.warn("数据表 {} 没有建立，尝试自动建立", helper.tableName());
                var db = new MysqlDatabase(this, clazz);
                db.createTable(false);
                // 尝试再次执行
                if (useSlaveServer)
                    query.openReadonly();
                else
                    query.open();
            }
        }
        query.setReadonly(true);
    }

    public boolean isEmpty() {
        return query.size() == 0;
    }

    public boolean isPresent() {
        return query.size() > 0;
    }

    // load.isPresentThrow: 载入一条数据，若不为空就抛出异常
    // isPresentThrow.update: 更新entity，若为空无法更新就抛出异常
    protected <X extends Throwable> EntityHome<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (query.size() > 0)
            throw exceptionSupplier.get();
        return this;
    }

    // load.isEmptyThrow: 载入一条数据，若为空就抛出异常
    protected <X extends Throwable> EntityHome<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (query.size() == 0)
            throw exceptionSupplier.get();
        return this;
    }

    protected T insert(Consumer<T> action) {
        T entity = helper.newEntity();
        action.accept(entity);
        this.insert(entity);
        return entity;
    }

    protected void insert(T entity) {
        query.setReadonly(false);
        try {
            helper.onInsertPostDefault(entity);
            entity.onInsertPost(query);
            query.append();
            query.current().loadFromEntity(entity);
            query.post();
            saveHistory(query, entity, HistoryTypeEnum.INSERT);
            query.current().saveToEntity(entity);
            entity.setEntityHome(this);
        } finally {
            query.setReadonly(true);
        }
    }

    @Override
    public void post(EntityImpl entity) {
        @SuppressWarnings("unchecked")
        T obj = (T) entity;
        int recNo = this.findRecNo(entity);
        if (recNo == 0)
            this.insert(obj);
        else {
            if (entity.getEntityHome() != this || entity.getClass() != this.clazz) {
                InvalidEntityException exception = new InvalidEntityException(
                        String.format("%s 不是 %s 亲自创建的类对象，不允许跨子类修改 %s", entity.getClass(), this, query.sqlText()));
                JayunLogParser.error(EntityHome.class, exception);
                log.info(exception.getMessage(), exception);
            }
            save(recNo - 1, obj);
            query.current().saveToEntity(obj);
        }
    }

    protected EntityHome<T> update(Consumer<T> action) {
        Objects.requireNonNull(action);
        T entity = null;
        for (int i = 0; i < query.size(); i++) {
            DataRow row = query.records().get(i);
            entity = row.asEntity(this.clazz);
            entity.setEntityHome(this);
            action.accept(entity);
            save(i, entity);
        }
        return this;
    }

    public int deleteIf(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        if (query.eof())
            return 0;
        query.setReadonly(false);
        try {
            var field = EntityHelper.get(clazz).lockedField();
            int result = 0;
            query.first();
            while (!query.eof()) {
                T entity = this.query.asEntity(clazz).orElseThrow();
                if (predicate.test(entity)) {
                    if (field.isPresent() && query.getBoolean(field.get().getName()))
                        throw new RuntimeException("record is locked");
                    saveHistory(query, entity, HistoryTypeEnum.DELETE);
                    query.delete();
                    result++;
                } else
                    query.next();
            }
            query.first();
            return result;
        } finally {
            query.setReadonly(true);
        }
    }

    /**
     * 返回entity在query中的序号，从1开始，若有找到则变更并返回recNo，否则返回0
     */
    @Override
    public int findRecNo(EntityImpl entity) {
        if (helper.idField().isEmpty())
            throw new IllegalArgumentException("id define not exists");
        Object idValue = helper.readIdValue(entity);
        if (idValue == null)
            return 0;

        String value = String.valueOf(idValue);

        // 优先判断是否为当前行
        DataRow current = query.current();
        if (current != null) {
            if (current.getString(helper.idFieldCode()).equals(value))
                return query.recNo();
            // 如果只有一条记录，就不要再找了
            if (query.size() == 1)
                return 1;
        }

        // 再全部记录均查找一次
        for (int i = 0; i < query.size(); i++) {
            DataRow row = query.records().get(i);
            if (row.getString(helper.idFieldCode()).equals(value)) {
                query.setRecNo(i + 1);
                return i + 1;
            }
        }
        return 0;
    }

    protected EntityHome<T> save(int index, T entity) {
        query.setRecNo(index + 1);
        if (!isCurrentRow(entity))
            throw new RuntimeException("recNo error, refuse update");
        query.setReadonly(false);
        try {
            Optional<Field> field = EntityHelper.get(clazz).lockedField();
            if (field.isPresent() && entity.isLocked() && query.getBoolean(field.get().getName()))
                throw new RuntimeException("record is locked, please unlock first");
            if (!isDataChanges(entity))
                return this;
            helper.onUpdatePostDefault(entity);
            entity.onUpdatePost(query);
            query.edit();
            query.current().loadFromEntity(entity);
            saveHistory(query, entity, HistoryTypeEnum.UPDATE);
            query.post();
        } finally {
            query.setReadonly(true);
        }
        return this;
    }

    protected boolean isDataChanges(T entity) {
        if (!isCurrentRow(entity))
            throw new RuntimeException("recNo error, refuse update");
        Map<String, Field> fields = helper.fields();
        T oldEntity = query.current().asEntity(clazz);
        for (String fieldCode : fields.keySet()) {
            try {
                Object newValue = fields.get(fieldCode).get(entity);
                Object oldValue = fields.get(fieldCode).get(oldEntity);
                if (!Objects.equals(newValue, oldValue))
                    return true; // 字段出现变更返回true
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false; // 字段没有发生变更返回false
    }

    protected void saveHistory(SqlQuery query, T entity, HistoryTypeEnum historyType) {
        boolean enableHistory = false;
        for (FieldMeta meta : this.query.fields()) {
            if (meta.history() != null && meta.history().master()) {
                enableHistory = true;
                break;
            }
        }
        if (enableHistory) {
            HistoryLoggerImpl logger = entity.getHistoryLogger();
            if (logger == null)
                logger = Application.getBean(HistoryLoggerImpl.class);
            if (logger != null)
                logger.save(query, historyType, entity.getClass());
        }
    }

    /**
     * @param entity Entity实体对象
     * @return 判断传入的entity对象，是不是当前记录
     */
    protected boolean isCurrentRow(T entity) {
        DataRow row = query.current();
        if (row == null)
            return false;

        if (helper.idField().isEmpty())
            throw new IllegalArgumentException("id define not exists");

        Object idValue = helper.readIdValue(entity);
        if (idValue == null)
            return false;

        return row.getString(helper.idFieldCode()).equals(String.valueOf(idValue));
    }

    @Override
    public void refresh(EntityImpl entity) {
        int recNo = this.findRecNo(entity);
        if (recNo == 0)
            throw new RuntimeException("refresh error, not find in query");
        query.current().saveToEntity(entity);
    }

    public EntityHome<T> setJoinName(EntityHome<? extends EntityImpl> join, String codeField, String nameField) {
        if (query.size() == 0)
            return this;

        Map<String, String> items = new HashMap<>();
        join.query.forEach(row -> items.put(row.getString(codeField), row.getString(nameField)));

        T entity = null;
        query.first();
        while (query.fetch()) {
            if (entity == null)
                entity = query.asEntity(this.clazz).orElseThrow();
            entity.onJoinName(query.current(), join.clazz, items);
        }
        return this;
    }

    public String sqlText() {
        return this.query.sqlText();
    }

    /**
     * 获取 EntityImpl 接口的所有实体实现类
     * 
     * @param clazz EntityImpl`接口的实现类
     * @return entity类家族
     */
    public static Set<Class<? extends EntityImpl>> getFamily(Class<?> clazz) {
        var items = new ArrayList<Class<? extends EntityImpl>>(); // 使用通配符泛型
        var classz = clazz;
        if (classz.getSuperclass().isAnnotationPresent(EntityKey.class))
            classz = classz.getSuperclass();
        items.add(classz.asSubclass(EntityImpl.class));

        for (var item : classz.getDeclaredClasses()) {
            if (EntityImpl.class.isAssignableFrom(item))
                items.add(item.asSubclass(EntityImpl.class));
        }

        var result = new HashSet<Class<? extends EntityImpl>>();
        for (var item : items) {
            if (item.isAnnotationPresent(EntityKey.class))
                result.add(item);
        }
        return result;
    }

}
