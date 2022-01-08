package cn.cerc.mis.ado;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class EntityQueryHelper<T> {
    private EntityQuery<T> query;

    public EntityQueryHelper(EntityQuery<T> query) {
        this.query = query;
    }

    public Optional<T> update(Consumer<? super T> action) {
        Objects.nonNull(action);
        if (query.eof())
            return Optional.empty();
        query.edit();
        T entity = query.currentEntity();
        action.accept(entity);
        query.current().loadFromEntity(entity);
        query.post();
        return Optional.of(entity);
    }

    public Optional<T> delete() {
        if (query.eof())
            return Optional.empty();
        T entity = query.currentEntity();
        query.delete();
        return Optional.of(entity);
    }

    public Optional<T> get() {
        return Optional.ofNullable(query.currentEntity());
    }

}
