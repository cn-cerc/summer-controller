package cn.cerc.mis.excel.output;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.config.ApplicationConfig;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * 批次导出单据明细
 */
public class BatchFormTemplate extends FormTemplate {
    private static final ClassResource res = new ClassResource(BatchFormTemplate.class, SummerMIS.ID);
    private static final DecimalFormat format = new DecimalFormat(ApplicationConfig.getPattern());
    private static final List<DataSet> items = new ArrayList<>();

    @Override
    public void output(WritableSheet sheet) throws WriteException {
        int newRow = 0;
        for (DataSet dataSet : items) {
            this.setDataSet(dataSet);
            this.setFooter((template, sheet1) -> {
                DataRow footer = new DataRow();
                for (DataRow item : dataSet) {
                    footer.setValue(res.getString(1, "合计数量"),
                            footer.getDouble(res.getString(1, "合计数量")) + item.getDouble("Num_"));
                    footer.setValue(res.getString(2, "合计金额"),
                            footer.getDouble(res.getString(2, "合计金额")) + item.getDouble("OriAmount_"));
                }
                int row = template.getRow();
                for (String field : footer.fields().names()) {
                    row++;
                    sheet1.addCell(new Label(0, row, field));
                    sheet1.addCell(new Label(1, row, format.format(new BigDecimal(footer.getString(field)))));
                }
            });

            // 输出原来的表格
            super.output(sheet);
            newRow += this.getHeads().size() + dataSet.size() + 6;
            this.setRow(newRow);
        }
    }

    public void add(DataSet item) {
        items.add(item);
    }

}
