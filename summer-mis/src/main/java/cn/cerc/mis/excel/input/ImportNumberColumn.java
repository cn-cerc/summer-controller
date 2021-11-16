package cn.cerc.mis.excel.input;

import cn.cerc.core.Utils;

import java.math.BigDecimal;

public class ImportNumberColumn extends ImportColumn {

    @Override
    public Object getValue() {
        double value = getRecord().getDouble(getCode());
        return Utils.formatFloat("0.######", new BigDecimal(Double.toString(value)).doubleValue());
    }

    @Override
    public boolean validate(int row, int col, String value) {
        String text = "0";
        if (!"".equals(value)) {
            text = String.valueOf(Math.abs(Double.parseDouble(value)));
        }
        return Utils.isNumeric(Utils.formatFloat("0.######", new BigDecimal(text).doubleValue()));
    }
}
