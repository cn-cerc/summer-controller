package cn.cerc.mis.ado;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataRowState;
import cn.cerc.core.DataSet;
import cn.cerc.core.FieldMeta;
import cn.cerc.core.FieldMeta.FieldKind;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.mssql.MssqlQuery;
import cn.cerc.db.mysql.MysqlQuery;
import cn.cerc.db.sqlite.SqliteQuery;
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
        SqlQuery query = createSqlQuery(handle);
        if (dataIn.curd()) {
            query.setJson(dataIn.json());
            query.setStorage(true);
            for (FieldMeta meta : query.fields()) {
                meta.setKind(FieldKind.Storage);
            }
            // 保存对数据表的修改
            query.operator().setTableName(this.table());
            query.operator().setUpdateKey(this.uid());
            // 先删除，再修改，最后增加，次序不要错
            saveDelete(dataIn, query);
            saveUpdate(dataIn, query);
            saveInsert(dataIn, query);
        }
        query.setCurd(false).records().clear();
        open(dataIn, query);
        // 对外输出meta
        query.fields().readDefine(this.getClass());
        query.setMetaInfo(true);
        return query.disableStorage().setState(ServiceState.OK);

    }

    private SqlQuery createSqlQuery(IHandle handle) {
        if (this.getClass().getAnnotation(Mssql.class) != null)
            return new MssqlQuery(handle);
        else if (this.getClass().getAnnotation(Mysql.class) != null)
            return new MysqlQuery(handle);
        else if (this.getClass().getAnnotation(Sqlite.class) != null)
            return new SqliteQuery();
        else
            throw new RuntimeException("unknow sql server type");
    }

    protected AdoTable open(DataSet dataIn, SqlQuery query) {
        DataRow headIn = dataIn.head();
        query.add("select * from %s", this.table());
        query.add("where 1=1");

        dataIn.head().remove("_sql_");

        for (FieldMeta meta : dataIn.head().getFields()) {
            String value = Utils.safeString(headIn.getString(meta.getCode())).trim();
            if (value.endsWith("*"))
                query.add("and %s like '%s%%'", meta.getCode(), value.substring(0, value.length() - 1));
            else if (!"*".equals(value))
                query.add("and %s='%s'", meta.getCode(), value);
        }
        query.open();
        return this;
    }

    protected void saveInsert(DataSet dataIn, SqlQuery query) {
        for (DataRow row : dataIn) {
            if (DataRowState.Insert == row.state()) {
                checkFieldsNull(row);
                query.append();
                query.copyRecord(row);
                query.post();
            }
        }
    }

    protected void saveUpdate(DataSet dataIn, SqlQuery query) {
        String uid = this.uid();
        for (DataRow row : dataIn) {
            if (DataRowState.Update == row.state()) {
                DataRow history = row.history();
                if (!query.locate(uid, history.getString(uid)))
                    throw new RuntimeException("update fail");
                checkFieldsNull(row);
                query.edit();
                query.copyRecord(row);
                query.post();
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
                if (column.nullable() && !row.has(field.getName()))
                    throw new RuntimeException(field.getName() + " can not be null");
            }
        }
    }

    public final String table() {
        String table = null;
        // 取得表名
        Table object = this.getClass().getDeclaredAnnotation(Table.class);
        if (object != null)
            table = object.name();
        if (Utils.isEmpty(table))
            table = this.getClass().getSimpleName();
        return table;
    }

    public final String uid() {
        String uid = "UID_";
        boolean has = false;
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getDeclaredAnnotation(Id.class) != null) {
                if (has)
                    throw new RuntimeException("暂不支持多个主键");
                uid = field.getName();
                has = true;
            }
        }
        return uid;
    }
}
