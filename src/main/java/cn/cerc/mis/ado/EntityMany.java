package cn.cerc.mis.ado;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;

public class EntityMany<T extends EntityImpl> extends EntityHome<T> implements Iterable<T> {

    public static <T extends EntityImpl> EntityMany<T> open(IHandle handle, Class<T> clazz, String... values) {
        return new EntityMany<T>(handle, clazz, SqlWhere.create(handle, clazz, values).build(), false, true);
    }

    public static <T extends EntityImpl> EntityMany<T> open(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityMany<T>(handle, clazz, sqlText, false, true);
    }

    public static <T extends EntityImpl> EntityMany<T> open(IHandle handle, Class<T> clazz,
                                                            Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityMany<T>(handle, clazz, where.build(), false, true);
    }

    public EntityMany(IHandle handle, Class<T> clazz, SqlText sql, boolean useSlaveServer, boolean writeCacheAtOpen) {
        super(handle, clazz, sql, useSlaveServer, writeCacheAtOpen);
    }

    @Override
    public <X extends Throwable> EntityMany<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X {
        super.isEmptyThrow(exceptionSupplier);
        return this;
    }

    @Override
    public <X extends Throwable> EntityMany<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        T entity = helper.newEntity();
        entity.setEntityHome(this);
        return entity;
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

    public EntityMany<T> updateAll(Consumer<T> action) {
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
        return query.records().stream().map(item -> {
            T entity = item.asEntity(clazz);
            entity.setEntityHome(this);
            return entity;
        });
    }

    @Override
    public Iterator<T> iterator() {
        return this.stream().collect(Collectors.toList()).iterator();
    }

    public <K> LinkedHashMap<K, T> map(Function<T, K> mapper) {
        LinkedHashMap<K, T> items = new LinkedHashMap<>();
        for (int i = 0; i < query.size(); i++) {
            T entity = this.get(i);
            items.put(mapper.apply(entity), entity);
        }
        return items;
    }

    public SqlQuery dataSet() {
        return query;
    }

}
