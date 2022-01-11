package cn.cerc.mis.ado;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cn.cerc.db.core.SqlQuery;

public interface EntityQueryList<T> {

    int size();

    T get(int index);

    // 增加
    T newEntity();

    void insert(T entity);

    // 删除
    void deleteAll();

    int deleteIf(Predicate<T> predicate);

    // 修改
    Optional<T> updateAll(Consumer<T> action);

    Optional<T> updateIf(Predicate<T> predicate);

    Stream<T> stream();

    SqlQuery dataSet();

}
