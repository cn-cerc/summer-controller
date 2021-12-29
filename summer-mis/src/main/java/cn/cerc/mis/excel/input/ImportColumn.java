package cn.cerc.mis.excel.input;

import cn.cerc.db.core.DataRow;

public abstract class ImportColumn {
    // 对应数据集字段名
    private String code;
    // 对应数据集字段标题
    private String name;
    // 数据源
    private DataRow record;

    // 取得数据
    public abstract Object getValue();

    public String getString() {
        return record.getString(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataRow getRecord() {
        return record;
    }

    public void setRecord(DataRow record) {
        this.record = record;
    }

    public abstract boolean validate(int row, int col, String value);
}