package cn.cerc.mis.excel;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.Utils;
import jxl.Cell;
import jxl.CellType;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public abstract class ExcelHelper implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ExcelHelper.class);
    private Workbook workbook;
    private int sheetNo;
    private ReadCellEvent onReadCell;

    public void loadFrom(String fileName) {
        File file = new File(fileName);
        try {
            // 获取Excel文件对象
            workbook = Workbook.getWorkbook(file);
        } catch (BiffException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void saveTo(DataSet dataSet, String fileName) {
        try {
            WritableWorkbook workbook = Workbook.createWorkbook(new File(fileName));
            // 创建新的一页
            WritableSheet sheet = workbook.createSheet("Sheet1", 0);
            // 输出列头
            int row = 0;
            int col = 0;
            for (FieldMeta meta : dataSet.fields()) {
                String value = meta.name() != null ? meta.name() : meta.code();
                Label item = new Label(col++, row, value);
                sheet.addCell(item);
            }
            // 输出内容
            dataSet.first();
            while (dataSet.fetch()) {
                row++;
                col = 0;
                for (FieldMeta meta : dataSet.fields()) {
                    String value = dataSet.getString(meta.code());
                    Label item = new Label(col++, row, value);
                    sheet.addCell(item);
                }
            }
            // 把创建的内容写入到输出流中，并关闭输出流
            workbook.write();
            workbook.close();
            //
            log.info("已生成：{}", fileName);
        } catch (WriteException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public interface ReadCellEvent {
        void writeValue(DataRow ds, Cell cell);
    }

    public void OnReadCell(ReadCellEvent event) {
        this.onReadCell = event;
    }

    protected void readCell(DataRow ds, int row, int col) {
        Cell cell = getSheet().getCell(col, row);
        if (onReadCell == null) {
            String value = cell.getContents();
            if (cell.getType() == CellType.NUMBER) {
                NumberCell numberCell = (NumberCell) cell;
                value = Utils.formatFloat("0.######", numberCell.getValue());
            }
            ds.setValue(asId(cell.getColumn()), value);
        } else {
            onReadCell.writeValue(ds, cell);
        }
    }

    @Override
    public void close() {
        if (workbook != null) {
            workbook.close();
            workbook = null;
        }
    }

    public int getSheetNo() {
        return sheetNo;
    }

    public void setSheetNo(int sheetNo) {
        this.sheetNo = sheetNo;
    }

    public Sheet getSheet() {
        return workbook.getSheet(sheetNo);
    }

    public static String asId(int col) {
        int high = (col - (col % 26)) / 26;
        if (high == 0)
            return String.valueOf((char) (col + 65));
        else
            return String.valueOf((char) (high + 64)) + String.valueOf((char) (col - high * 26 + 65));
    }

    public static int asCol(String id) {
        if (id.length() == 1)
            return id.charAt(0) - 65;
        else if (id.length() == 2)
            return (id.charAt(0) - 64) * 26 + id.charAt(1) - 65;
        else
            return -1;
    }

    public static void main(String[] args) {
        System.out.println(asCol("A"));
        System.out.println(asCol("Z"));
        System.out.println(asCol("AA"));
        System.out.println(asCol("AB"));
        System.out.println(asId(asCol("A")));
        System.out.println(asId(asCol("Z")));
        System.out.println(asId(asCol("AA")));
        System.out.println(asId(asCol("AB")));
    }

}
