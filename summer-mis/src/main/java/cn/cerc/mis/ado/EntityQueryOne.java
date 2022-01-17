package cn.cerc.mis.ado;

import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlText;

public class EntityQueryOne<T extends EntityImpl> extends EntityQuery<T> {

    public EntityQueryOne(IHandle handle, Class<T> clazz, SqlText sql, boolean useSlaveServer,
            boolean writeCacheAtOpen) {
        super(handle, clazz, sql, useSlaveServer, writeCacheAtOpen);
        if (query.size() > 1)
            throw new RuntimeException("There're too many records.");
    }

    @Override
    public <X extends Throwable> EntityQueryOne<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isEmptyThrow(exceptionSupplier);
        return this;
    }

    @Override
    public <X extends Throwable> EntityQueryOne<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
    public EntityQueryOne<T> update(Consumer<T> action) {
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

}
