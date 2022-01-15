package cn.cerc.mis.ado;

import java.lang.reflect.Field;
import java.util.Map;

import javax.persistence.Column;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataRowState;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.FieldMeta.FieldKind;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISqlDatabase;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.db.mysql.MysqlDatabase;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceState;

public abstract class AdoTable implements IService {

    public DataSet execute(IHandle handle, DataSet dataIn) {
        // 检查必备的查询参数
        DataRow headIn = dataIn.head();
        for (Field field : this.getClass().getDeclaredFields()) {
            Search search = field.getAnnotation(Search.class);
            if (search != null && !headIn.has(field.getName()))
                return new DataSet().setMessage(field.getName() + " can not be null");
        }

        // 打开数据表
        if (dataIn.crud()) {
            SqlQuery query = buildQuery(handle);
            query.setJson(dataIn.json());
            query.setStorage(true);
            for (FieldMeta meta : dataIn.fields())
                meta.setKind(FieldKind.Storage);

            // 保存对数据表的修改
            query.operator().setTable(Utils.findTable(this.getClass()));
            query.operator().setOid(Utils.findOid(this.getClass(), MysqlDatabase.DefaultOID));
            // 先删除，再修改，最后增加，次序不要错
            saveDelete(dataIn, query);
            saveUpdate(dataIn, query);
            saveInsert(dataIn, query);
        }
        SqlQuery query = buildQuery(handle);
        open(dataIn, query);
        // 对外输出meta
        query.fields().readDefine(this.getClass());
        query.setMeta(true);
        return query.disableStorage().setState(ServiceState.OK);
    }

    public SqlQuery buildQuery(IHandle handle) {
        Class<? extends AdoTable> clazz = this.getClass();
        SqlServer sqlServer = clazz.getAnnotation(SqlServer.class);
        if (sqlServer == null)
            throw new RuntimeException("unknow sql server");

        ISqlDatabase database = EntityQuery.findDatabase(handle, clazz);
        SqlServer server = clazz.getAnnotation(SqlServer.class);
        SqlServerType sqlServerType = (server != null) ? server.type() : SqlServerType.Mysql;
        SqlQuery query = new SqlQuery(handle, sqlServerType);
        EntityQuery.registerCacheListener(query, clazz, true);
        query.operator().setTable(database.table());
        query.operator().setOid(database.oid());
        return query;
    }

    protected AdoTable open(DataSet dataIn, SqlQuery query) {
        DataRow headIn = dataIn.head();
        query.add("select * from %s", this.table());
        query.add("where 1=1");

        dataIn.head().remove("_sql_");

        for (FieldMeta meta : dataIn.head().getFields()) {
            if (!headIn.has(meta.code())) {
                continue;
            }
            String value = Utils.safeString(headIn.getString(meta.code())).trim();
            if (value.endsWith("*"))
                query.add("and %s like '%s%%'", meta.code(), value.substring(0, value.length() - 1));
            else if (!"*".equals(value))
                query.add("and %s='%s'", meta.code(), value);
        }
        query.open();
        return this;
    }

    protected void saveInsert(DataSet dataIn, SqlQuery query) {
        for (DataRow row : dataIn) {
            if (DataRowState.Insert == row.state()) {
                checkFieldsNull(row);
                try {
                    query.insertStorage(row);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    protected void saveUpdate(DataSet dataIn, SqlQuery query) {
        String uid = Utils.findOid(this.getClass(), query.operator().oid());
        for (DataRow row : dataIn) {
            if (DataRowState.Update == row.state()) {
                DataRow history = row.history();
                if (!query.locate(uid, history.getString(uid)))
                    throw new RuntimeException("update fail");
                checkFieldsNull(row);
                try {
                    query.updateStorage(row);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    protected void saveDelete(DataSet dataIn, SqlQuery query) {
        for (DataRow row : dataIn.garbage()) {
            try {
                for (FieldMeta meta : row.fields())
                    meta.setKind(FieldKind.Storage);
                query.deleteStorage(row);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    protected void checkFieldsNull(DataRow row) {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                if (!column.nullable() && !row.has(field.getName()))
                    throw new RuntimeException(field.getName() + " can not be null");
            }
        }
    }

    public String table() {
        return Utils.findTable(this.getClass());
    }

    /**
     * 插入新记录时，判断字段是否允许为空，若不允许为空，则设置默值
     * 
     * @param handle IHandle
     */
    public void insertTimestamp(IHandle handle) {
        Map<String, Field> items = DataRow.getEntityFields(this.getClass());
        Variant variant = new Variant();
        try {
            for (Field field : items.values()) {
                Column column = field.getAnnotation(Column.class);
                if ((column != null && field.get(this) == null)) {
                    Describe describe = field.getAnnotation(Describe.class);
                    String def = describe != null ? describe.def() : null;
                    if (!column.nullable() || !Utils.isEmpty(def))
                        variant.setData(def).writeToEntity(this, field);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新记录时自动更新时间戳
     * 
     * @param handle IHandle
     */
    public void updateTimestamp(IHandle handle) {

    }

}
