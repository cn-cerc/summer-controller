package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;

public interface EntityQueryOne<T> {

    boolean isEmpty();

    boolean isPresent();

    Optional<T> get();
    
    void save(T entity);

    EntityQueryOne<T> orElseInsert(Consumer<T> action);

    <X extends Throwable> EntityQueryOne<T> orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    EntityQueryOne<T> update(Consumer<T> action);

    EntityQueryOne<T> delete();

    DataRow current();
}
