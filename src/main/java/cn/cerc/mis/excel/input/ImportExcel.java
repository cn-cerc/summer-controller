package cn.cerc.mis.excel.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.excel.ExcelCellReader;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ImportExcel extends ImportExcelFile {
    private static final ClassResource res = new ClassResource(ImportExcel.class, SummerMIS.ID);

    private static ApplicationContext app;
    private static String xmlFile = "classpath:import-excel.xml";
    private HttpServletResponse response;
    private String templateId;
    private ImportExcelTemplate template;
    private ImportError errorHandle;
    private ImportRecord readHandle;

    public ImportExcel(HttpServletRequest request, HttpServletResponse response) {
        super();
        this.setRequest(request);
        this.response = response;
    }

    public void exportTemplate() throws IOException, WriteException {
        DataSet dataOut = dataSet();
        this.setResponse(response);
        OutputStream outputStream = response.getOutputStream();// 取得输出流
        response.reset();// 清空输出流

        template = this.getTemplate();
        // 下面是对中文文件名的处理
        response.setCharacterEncoding("UTF-8");// 设置相应内容的编码格式
        String fname = URLEncoder.encode(template.getFileName(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fname + ".xls");
        response.setContentType("application/msexcel");// 定义输出类型

        List<ImportColumn> columns = template.getColumns();
        WorkbookSettings settings = new WorkbookSettings();
        settings.setGCDisabled(true);
        // 创建工作薄
        WritableWorkbook workbook = jxl.Workbook.createWorkbook(outputStream, settings);

        // 创建新的一页
        WritableSheet sheet = workbook.createSheet("First Sheet", 0);

        // 输出列头
        int row = 0;
        for (int col = 0; col < columns.size(); col++) {
            ImportColumn column = columns.get(col);
            Label item = new Label(col, row, column.getName());
            sheet.addCell(item);
        }

        // 输出列数据
        if (dataOut != null) {
            dataOut.first();
            while (dataOut.fetch()) {
                row++;
                for (int col = 0; col < columns.size(); col++) {
                    ImportColumn column = columns.get(col);
                    column.setRecord(dataOut.current());
                    if (column instanceof ImportNumberColumn) {
                        jxl.write.Number item = new jxl.write.Number(col, row, (double) column.getValue());
                        sheet.addCell(item);
                    } else {
                        Label item = new Label(col, row, (String) column.getValue());
                        sheet.addCell(item);
                    }
                }
            }
        }

        // 把创建的内容写入到输出流中，并关闭输出流
        workbook.write();
        workbook.close();
        outputStream.close();
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public ImportExcelTemplate getTemplate() {
        if (template == null) {
            if (templateId == null) {
                throw new RuntimeException("templateId is null");
            }
            if (app == null) {
                app = new FileSystemXmlApplicationContext(xmlFile);
            }
            template = app.getBean(templateId, ImportExcelTemplate.class);
        }
        return template;
    }

    public void setTemplate(ImportExcelTemplate template) {

        this.template = template;
    }

    public String getTemplateId() {
        return templateId;
    }

    public ImportExcel setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    public DataSet readFileData(DataRow record) throws Exception {
        FileItem file = this.getFile(record);
        DataSet ds = new DataSet();
        if (file.getName().endsWith(".csv")) {
            readFileFromCSV(file, ds);
        } else {
            readFileFromExcel(file, ds);
        }
        return ds;
    }

    private void readFileFromCSV(FileItem file, DataSet ds) throws UnsupportedEncodingException, IOException {
        InputStreamReader isr = new InputStreamReader(file.getInputStream(), "GBK");
        BufferedReader reader = new BufferedReader(isr);
        // 即 \G(?:^|,)(?:"([^"]*+(?:""[^"]*+)*+)"|([^",]*+))，解析CSV
        String reg = "\\G(?:^|,)(?:\"([^\"]*+(?:\"\"[^\"]*+)*+)\"|([^\",]*+))";
        Matcher matcherMain = Pattern.compile(reg).matcher("");
        Matcher matcherQuoto = Pattern.compile("\"\"").matcher("");
        List<String> fields = null;
        String readLine;
        int i = 0;
        while ((readLine = reader.readLine()) != null) {
            matcherMain.reset(readLine);
            if (i == 0) {
                fields = new ArrayList<>();
                while (matcherMain.find()) {
                    String field;
                    if (matcherMain.start(2) >= 0) {
                        field = matcherMain.group(2);
                    } else {
                        field = matcherQuoto.reset(matcherMain.group(1)).replaceAll("\"");
                    }
                    fields.add(field);
                }
            } else {
                ds.append();
                List<String> valList = new ArrayList<>();
                while (matcherMain.find()) {
                    String value;
                    if (matcherMain.start(2) >= 0) {
                        value = matcherMain.group(2);
                    } else {
                        value = matcherQuoto.reset(matcherMain.group(1)).replaceAll("\"");
                    }
                    valList.add(value);
                }
                for (int j = 0; j < valList.size(); j++) {
                    if (!Utils.isEmpty(fields.get(j)))
                        ds.setValue(fields.get(j), valList.get(j));
                }
            }
            i++;
        }
    }

    private void readFileFromExcel(FileItem file, DataSet ds) throws Exception {
        Workbook workbook = null;
        if (file.getName().endsWith(".xls"))
            workbook = new HSSFWorkbook(file.getInputStream());
        if (file.getName().endsWith(".xlsx"))
            workbook = new XSSFWorkbook(file.getInputStream());
        if (workbook == null)
            throw new RuntimeException(String.format("导入的文件：<b>%s</b>, 不是xls或xlsx文件无法导入！", file.getName()));

        // 获取文件的指定工作表 默认的第一个
        Sheet sheet = workbook.getSheetAt(0);

        int rows = sheet.getLastRowNum() + 1;
        int columns = StreamSupport.stream(sheet.spliterator(), false).mapToInt(Row::getLastCellNum).max().orElse(0);

        ImportExcelTemplate template = this.getTemplate();
        if (template.getColumns().size() != columns) {
            throw new RuntimeException(
                    String.format(res.getString(1, "导入的文件：<b>%s</b>, 其总列数为 %d，而模版总列数为  %d 二者不一致，无法导入！"), file.getName(),
                            columns, template.getColumns().size()));
        }

        for (int row = 0; row < rows; row++) {
            if (row == 0) {
                for (int col = 0; col < columns; col++) {
                    Cell cell = sheet.getRow(row).getCell(col);
                    if (cell == null)
                        continue;
                    String value = ExcelCellReader.getString(cell);
                    String title = template.getColumns().get(col).getName();
                    if (!title.equals(value)) {
                        throw new RuntimeException(
                                String.format(res.getString(2, "导入的文件：<b>%s</b>，其标题第 %d 列为【 %s】, 模版中为【%s】，二者不一致，无法导入！"),
                                        file.getName(), col + 1, value, title));
                    }
                }
            } else {
                ds.append();
                for (int col = 0; col < columns; col++) {
                    Cell cell = sheet.getRow(row).getCell(col);
                    if (cell == null)
                        continue;
                    String value = ExcelCellReader.getString(cell);
                    if (cell.getCellType() == CellType.NUMERIC) {
                        if (!DateUtil.isCellDateFormatted(cell)) {
                            double d = ExcelCellReader.getDouble(cell);
                            value = Utils.formatFloat("0.######", d);
                        }
                    }
                    ImportColumn column = template.getColumns().get(col);
                    if (!column.validate(row, col, value)) {
                        ColumnValidateException err = new ColumnValidateException(
                                String.format(res.getString(3, "其数据不符合模版要求，当前值为：%s"), value));
                        err.setTitle(column.getName());
                        err.setValue(value);
                        err.setCol(col);
                        err.setRow(row);
                        if (errorHandle == null || !errorHandle.process(err)) {
                            throw err;
                        }
                    }
                    ds.setValue(column.getCode(), value);
                }
                if (readHandle != null && !readHandle.process(ds.current())) {
                    break;
                }
            }
        }
    }

    public ImportError getErrorHandle() {
        return errorHandle;
    }

    public void setErrorHandle(ImportError errorHandle) throws Exception {
        this.errorHandle = errorHandle;
    }

    public ImportRecord getReadHandle() {
        return readHandle;
    }

    public void setReadHandle(ImportRecord readHandle) {
        this.readHandle = readHandle;
    }

    public void readRecords(ImportRecord readHandle) throws Exception {
        this.setReadHandle(readHandle);
        DataSet ds = dataSet();
        while (ds.fetch()) {
            readFileData(ds.current());
        }
    }
}
