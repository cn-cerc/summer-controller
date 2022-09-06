package cn.cerc.mis.ado;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;

public class EntityOne<T extends EntityImpl> extends EntityHome<T> {
    private static final Logger log = LoggerFactory.getLogger(EntityOne.class);

    public static <T extends EntityImpl> EntityOne<T> open(IHandle handle, Class<T> clazz, String... values) {
        SqlText sql = SqlWhere.create(handle, clazz, values).build();
        return new EntityOne<T>(handle, clazz, sql, false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> open(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityOne<T>(handle, clazz, sqlText, false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> open(IHandle handle, Class<T> clazz,
            Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityOne<T>(handle, clazz, where.build(), false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> open(IHandle handle, Class<T> clazz, long uid) {
        SqlText sql = SqlWhere.create(clazz).eq("UID_", uid).build();
        return new EntityOne<T>(handle, clazz, sql, false, false);
    }

    public EntityOne(IHandle handle, Class<T> clazz, SqlText sql, boolean useSlaveServer, boolean writeCacheAtOpen) {
        super(handle, clazz, sql, useSlaveServer, writeCacheAtOpen);
        if (query.size() > 1) {
            log.error("There are too many records. Entity {} sqlText {}", clazz.getName(), sql.text());
            throw new RuntimeException("There are too many records.");
        }
    }

    @Override
    public <X extends Throwable> EntityOne<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isEmptyThrow(exceptionSupplier);
        return this;
    }

    @Override
    public <X extends Throwable> EntityOne<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isPresentThrow(exceptionSupplier);
        return this;
    }

    public T get() {
        if (query.size() == 0)
            return null;
        T entity = query.records().get(0).asEntity(clazz);
        entity.setEntityHome(this);
        return entity;
    }

    // 取得entity，若取不到就抛出异常
    public <X extends Throwable> T getElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (query.size() == 0)
            throw exceptionSupplier.get();
        return query.records().get(0).asEntity(clazz);
    }

    // update.orElseInsert: 更新entity，若为空无法更新就执行插入
    @Override
    public EntityOne<T> update(Consumer<T> action) {
        super.update(action);
        return this;
    }

    // loadOne.orElseInsert: 载入一条数据，若为空就执行插入
    public T orElseInsert(Consumer<T> action) {
        if (this.isEmpty())
            return super.insert(action);
        else
            return this.get();
    }

    public T delete() {
        if (query.size() == 0)
            return null;
        query.setReadonly(false);
        T entity = null;
        try {
            entity = query.current().asEntity(clazz);
            query.delete();
        } finally {
            query.setReadonly(true);
        }
        return entity;
    }

    public DataRow current() {
        return query.current();
    }

    public DataSet dataSet() {
        return query;
    }

}
