package cn.cerc.mis.ado;

import java.util.Objects;
import java.util.function.Consumer;

import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.core.SqlWhere;

public class Query {

    public static <T extends EntityImpl> EntityOne<T> get(IHandle handle, Class<T> clazz, String... values) {
        SqlText sql = SqlWhere.create(handle, clazz, values).build();
        return new EntityOne<T>(handle, clazz, sql, false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> get(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityOne<T>(handle, clazz, sqlText, false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> get(IHandle handle, Class<T> clazz, Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityOne<T>(handle, clazz, where.build(), false, false);
    }

    public static <T extends EntityImpl> EntityOne<T> get(IHandle handle, Class<T> clazz, long uid) {
        SqlText sql = SqlWhere.create(clazz).eq("UID_", uid).build();
        return new EntityOne<T>(handle, clazz, sql, false, false);
    }

    public static <T extends EntityImpl> EntityMany<T> list(IHandle handle, Class<T> clazz, String... values) {
        return new EntityMany<T>(handle, clazz, SqlWhere.create(handle, clazz, values).build(), false, true);
    }

    public static <T extends EntityImpl> EntityMany<T> list(IHandle handle, Class<T> clazz, SqlText sqlText) {
        return new EntityMany<T>(handle, clazz, sqlText, false, true);
    }

    public static <T extends EntityImpl> EntityMany<T> list(IHandle handle, Class<T> clazz,
            Consumer<SqlWhere> consumer) {
        Objects.requireNonNull(consumer);
        SqlWhere where = SqlWhere.create(handle, clazz);
        consumer.accept(where);
        return new EntityMany<T>(handle, clazz, where.build(), false, true);
    }

}
