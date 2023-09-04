package cn.cerc.mis.math;

import cn.cerc.db.core.DataRow;

public class FunctionField implements IFunction {
    private DataRow dataRow;

    public FunctionField(DataRow dataRow) {
        this.dataRow = dataRow;
    }

    @Override
    public boolean isName(String name) {
        return dataRow.fields().exists(name);
    }

    @Override
    public String process(FunctionManager manage, String text) {
        return dataRow.getString(text);
    }

}
