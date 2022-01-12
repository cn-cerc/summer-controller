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

    EntityQueryOne<T> ifEmptyInsert(Consumer<T> action);

    <X extends Throwable> EntityQueryOne<T> orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    Optional<T> update(Consumer<T> action);

    boolean delete();

    DataRow current();
}
