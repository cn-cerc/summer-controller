package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;

public interface EntityQueryOne<T> {

    boolean isEmpty();

    boolean isPresent();

    // get.orElseThrow: 取得entity，若取不到就抛出异常
    Optional<T> get();

    void save(T entity);

    // update.orElseInsert: 更新entity，若为空无法更新就执行插入
    // update.orElseThrow: 更新entity，若为空无法更新就抛出异常
    EntityQueryOne<T> update(Consumer<T> action);

    // delete.orElseThrow: 删除entity，若为空无法删除就抛出异常
    Optional<T> delete();

    // loadOne.orElseInsert: 载入一条数据，若为空就执行插入
    EntityQueryOne<T> orElseInsert(Consumer<T> action);

    // loadOne.orElseThrow: 载入一条数据，若为空就抛出异常
    <X extends Throwable> EntityQueryOne<T> orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    DataRow current();

}
