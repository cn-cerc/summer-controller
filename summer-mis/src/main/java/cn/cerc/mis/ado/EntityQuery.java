package cn.cerc.mis.ado;

import cn.cerc.db.core.DataSetGson;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.Utils;

public class EntityQuery<T> extends SqlQuery implements IHandle {
    private static final long serialVersionUID = 8276125658457479833L;
    private Class<T> clazz;

    public static <U> EntityQuery<U> create(IHandle handle, Class<U> clazz) {
        ISqlDatabase database = EntityManager.findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        EntityQuery<U> query = new EntityQuery<U>(handle, clazz, sqlServerType, true);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        return query;
    }

    private EntityQuery(IHandle handle, Class<T> clazz, SqlServerType sqlServerType, boolean writeCacheAtOpen) {
        super(handle, sqlServerType);
        this.clazz = clazz;
        EntityManager.registerCacheListener(this, clazz, writeCacheAtOpen);
    }

    @Override
    public String json() {
        return new DataSetGson<EntityQuery<T>>(this).encode();
    }

    @Override
    public EntityQuery<T> setJson(String json) {
        this.clear();
        if (!Utils.isEmpty(json))
            new DataSetGson<EntityQuery<T>>(this).decode(json);
        return this;
    }

    public String table() {
        return Utils.findTable(clazz);
    }

}
