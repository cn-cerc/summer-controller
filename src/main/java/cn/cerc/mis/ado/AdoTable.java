package cn.cerc.mis.ado;

import java.lang.reflect.Field;

import javax.persistence.Column;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataRowState;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.EntityHelper;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.FieldMeta.FieldKind;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceState;

public abstract class AdoTable extends CustomEntity implements IService {

    public DataSet execute(IHandle handle, DataSet dataIn) {
        // 检查必备的查询参数
        DataRow headIn = dataIn.head();
        for (Field field : this.getClass().getDeclaredFields()) {
            Search search = field.getAnnotation(Search.class);
            if (search != null && !headIn.hasValue(field.getName()))
                return new DataSet().setMessage(field.getName() + " can not be null");
        }

        // 打开数据表
        if (dataIn.crud()) {
            SqlQuery query = buildQuery(handle);
            query.setJson(dataIn.json());
            query.setStorage(true);
            for (FieldMeta meta : dataIn.fields())
                meta.setKind(FieldKind.Storage);

            EntityHelper<? extends AdoTable> helper = EntityHelper.get(this.getClass());
            // 保存对数据表的修改
            query.operator().setTable(helper.tableName());
            query.operator().setOid(helper.idFieldCode());
            query.operator().setVersionField(helper.versionFieldCode());
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
        SqlServer sqlServer = EntityHelper.get(clazz).sqlServer();
        if (sqlServer == null)
            throw new RuntimeException("unknow sql server");

        EntityHelper<? extends AdoTable> helper = EntityHelper.get(clazz);
        SqlQuery query = new SqlQuery(handle, helper.sqlServerType());
        query.operator().setTable(helper.tableName());
        query.operator().setOid(helper.idFieldCode());
        query.operator().setVersionField(helper.versionFieldCode());
        EntityHome.registerCacheListener(query, clazz, true);
        return query;
    }

    protected AdoTable open(DataSet dataIn, SqlQuery query) {
        DataRow headIn = dataIn.head();
        query.add("select * from %s", this.table());
        query.add("where 1=1");

        dataIn.head().remove("_sql_");

        for (FieldMeta meta : dataIn.head().getFields()) {
            if (!headIn.hasValue(meta.code())) {
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
        String uid = EntityHelper.get(this.getClass()).idFieldCode();
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
                if (!column.nullable() && !row.hasValue(field.getName()))
                    throw new RuntimeException(field.getName() + " can not be null");
            }
        }
    }

    public String table() {
        return EntityHelper.get(this.getClass()).tableName();
    }

}
