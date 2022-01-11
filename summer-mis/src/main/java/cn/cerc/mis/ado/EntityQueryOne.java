package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.function.Consumer;

import cn.cerc.db.core.DataRow;

public interface EntityQueryOne<T> {

    boolean isEmpty();
    
    boolean isPresent();

    Optional<T> get();

    EntityQueryOne<T> ifEmptyInsert(Consumer<T> action);

    Optional<T> update(Consumer<T> action);

    boolean delete();

    DataRow current();
}
