package cn.cerc.mis.excel.input;

import cn.cerc.db.core.Datetime;

public class ImportDateColumn extends ImportColumn {

    @Override
    public Object getValue() {
        return getRecord().getDatetime(getCode()).getDate();
    }

    @Override
    public boolean validate(int row, int col, String value) {
        return !(new Datetime(value)).isEmpty();
    }
}
