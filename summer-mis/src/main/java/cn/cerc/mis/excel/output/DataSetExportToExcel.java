package cn.cerc.mis.excel.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.Utils;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * 将DataSet数据转换成Excel文件流
 */
public class DataSetExportToExcel {

    public static void output(DataSet dataSet, OutputStream outputstream) throws IOException, WriteException {
        dataSet.setMeta(true);
        dataSet.buildMeta();

        int row = 0;
        // 创建工作簿
        WritableWorkbook workbook = Workbook.createWorkbook(outputstream);

        // 创建新表单
        WritableSheet sheet = workbook.createSheet("Sheet1", 0);

        // 输出列头
        HashSet<FieldMeta> metas = dataSet.fields().getItems();
        int col = 0;
        for (FieldMeta meta : metas) {
            Label item = new Label(col, row, meta.name());
            sheet.addCell(item);
            col++;
        }

        // 输出列身
        dataSet.first();
        while (dataSet.fetch()) {
            row++;
            DataRow dataRow = dataSet.current();
            build(sheet, dataRow, metas, row);
        }

        workbook.write();
        workbook.close();
    }

    private static void build(WritableSheet sheet, DataRow dataRow, HashSet<FieldMeta> metas, int row) throws WriteException {
        int i = 0;
        for (FieldMeta meta : metas) {
            String type = meta.typeValue();
            if (!Utils.isEmpty(type))
                type = type.substring(0, 1);
            if (Utils.isEmpty(type))
                type = "s";
            WritableCell item;
            switch (type) {
            case "n":
                item = new jxl.write.Number(i, row, dataRow.getInt(meta.code()));
                break;
            case "f":
                item = new jxl.write.Number(i, row, dataRow.getDouble(meta.code()));
                break;
            case "s":
            default:
                item = new Label(i, row, dataRow.getString(meta.code()));
                break;
            }
            sheet.addCell(item);
            i++;
        }
    }

}
