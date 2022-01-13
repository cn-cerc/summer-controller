package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;

public interface EntityQueryOne<T> {

    boolean isEmpty();

    // loadOne.isEmptyThrow: 载入一条数据，若为空就抛出异常
    <X extends Throwable> EntityQueryOne<T> isEmptyThrow(Supplier<? extends X> exceptionSupplier) throws X;

    boolean isPresent();

    // loadOne.isPresentThrow: 载入一条数据，若不为空就抛出异常
    // isPresentThrow.update: 更新entity，若为空无法更新就抛出异常
    <X extends Throwable> EntityQueryOne<T> isPresentThrow(Supplier<? extends X> exceptionSupplier) throws X;

    T get();

    // 取得entity，若取不到就抛出异常
    <X extends Throwable> T getElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    void save(T entity);

    // update.orElseInsert: 更新entity，若为空无法更新就执行插入
    EntityQueryOne<T> update(Consumer<T> action);

    // loadOne.orElseInsert: 载入一条数据，若为空就执行插入
    EntityQueryOne<T> orElseInsert(Consumer<T> action);

    // delete.orElseThrow: 删除entity，若为空无法删除就抛出异常
    Optional<T> delete();

    DataRow current();

}
