package cn.cerc.mis.excel.input;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Ignore;
import org.junit.Test;

import cn.cerc.db.core.DataSet;

public class ReadExcelToDataSetTest {

    public static String[] colHead = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF", "AG",
            "AH", "AI", "AJ", "AK", "AL", "AM", "AN", "AO", "AP", "AQ", "AR", "AS", "AT", "AU", "AV", "AW", "AX", "AY",
            "AZ", "BA", "BB", "BC", "BD", "BE", "BF", "BG", "BH", "BI" };

    @Test
    @Ignore
    public void test() {
        try {
            File file = new File("/home/h2syj/Documents/old_word/2022-11-08_222031_导入车辆保险.xls");
            FileInputStream fileInputStream = new FileInputStream(file);

            DataSet dataOut = new DataSet();

            // Create Workbook instance holding reference to .xlsx file
            String fileName = file.getName();
            Workbook workbook = null;
            if (fileName.endsWith(".xls"))
                workbook = new HSSFWorkbook(fileInputStream);
            if (fileName.endsWith(".xlsx"))
                workbook = new XSSFWorkbook(fileInputStream);

            // Get first/desired sheet from the workbook
            Sheet sheet = workbook.getSheetAt(0);

            int maxCellNum = StreamSupport.stream(sheet.spliterator(), false)
                    .mapToInt(Row::getLastCellNum)
                    .max()
                    .orElse(0);
            System.out.println(String.format("表格总行数：%s，表格总列数：%s", sheet.getLastRowNum() + 1, maxCellNum));
            // Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                dataOut.append();

                // For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();

                int col = 0;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                    case NUMERIC:
                        dataOut.setValue(colHead[col++], cell.getNumericCellValue());
                        break;
                    default:
                        dataOut.setValue(colHead[col++], cell.getStringCellValue());
                        break;
                    }
                }
            }
            fileInputStream.close();

//            System.out.println(dataOut.json());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
