package cn.cerc.mis.ado;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlText;

public class EntityQueryAll<T extends EntityImpl> extends EntityQuery<T> implements Iterable<T> {

    public EntityQueryAll(IHandle handle, Class<T> clazz, SqlText sql, boolean useSlaveServer,
            boolean writeCacheAtOpen) {
        super(handle, clazz, sql, useSlaveServer, writeCacheAtOpen);
    }

    @Override
    public <X extends Throwable> EntityQueryAll<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isEmptyThrow(exceptionSupplier);
        return this;
    }

    @Override
    public <X extends Throwable> EntityQueryAll<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isPresentThrow(exceptionSupplier);
        return this;
    }

    public int size() {
        return query.size();
    }

    @Override
    public T insert(Consumer<T> action) {
        return super.insert(action);
    }

    public T newEntity() {
        return helper.newEntity();
    }

    public void insert(List<T> list) {
        for (T entity : list)
            insert(entity);
    }

    public T get(int index) {
        T entity = query.records().get(index).asEntity(clazz);
        entity.setEntityHome(this);
        return entity;
    }

    public EntityQueryAll<T> updateAll(Consumer<T> action) {
        super.update(action);
        return this;
    }

    public void deleteAll() {
        query.setReadonly(false);
        try {
            query.first();
            while (!query.eof())
                query.delete();
        } finally {
            query.setReadonly(true);
        }
    }

    public void deleteAll(List<T> list) {
        query.setReadonly(false);
        try {
            for (T entity : list) {
                if (entity.findRecNo() < 0)
                    throw new RuntimeException("delete fail, entity not in query");
                query.delete();
            }
        } finally {
            query.setReadonly(true);
        }
    }

    public Stream<T> stream() {
        return query.records().stream().map(item -> item.asEntity(clazz));
    }

    public SqlQuery dataSet() {
        return query;
    }

    @Override
    public Iterator<T> iterator() {
        return this.stream().iterator();
    }

}
