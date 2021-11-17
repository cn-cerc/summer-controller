package cn.cerc.mis.excel.output;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
import cn.cerc.core.FieldMeta;
import cn.cerc.core.Utils;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

/**
 * 将DataSet数据转换成Excel文件流
 */
public class DataSetExportToExcel {

    public static void output(DataSet dataSet, OutputStream outputstream) throws IOException, WriteException {
        dataSet.setMetaInfo(true);
        dataSet.buildMeta();

        int row = 0;
        // 创建工作簿
        WritableWorkbook workbook = Workbook.createWorkbook(outputstream);

        // 创建新表单
        WritableSheet sheet = workbook.createSheet("Sheet1", 0);

        // 输出列头
        HashSet<FieldMeta> metas = dataSet.getFieldDefs().getItems();
        int col = 0;
        for (FieldMeta meta : metas) {
            Label item = new Label(col, row, meta.getName());
            sheet.addCell(item);
            col++;
        }

        // 输出列身
        dataSet.first();
        while (dataSet.fetch()) {
            row++;
            DataRow dataRow = dataSet.getCurrent();
            build(sheet, dataRow, metas, row);
        }

        workbook.write();
        workbook.close();
    }

    private static void build(WritableSheet sheet, DataRow dataRow, HashSet<FieldMeta> metas, int row) throws WriteException {
        int i = 0;
        for (FieldMeta meta : metas) {
            String type = meta.getType();
            if (!Utils.isEmpty(type))
                type = type.substring(0, 1);
            if (Utils.isEmpty(type))
                type = "s";
            WritableCell item;
            switch (type) {
            case "n":
                item = new jxl.write.Number(i, row, dataRow.getInt(meta.getCode()));
                break;
            case "f":
                item = new jxl.write.Number(i, row, dataRow.getDouble(meta.getCode()));
                break;
            case "s":
            default:
                item = new Label(i, row, dataRow.getString(meta.getCode()));
                break;
            }
            sheet.addCell(item);
            i++;
        }
    }

}
