package cn.cerc.mis.excel.output;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.FastDate;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.Utils;
import cn.cerc.db.mongo.MongoOSS;
import cn.cerc.db.oss.OssConnection;
import cn.cerc.mis.config.ApplicationConfig;
import jxl.write.DateFormat;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableImage;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

public class ExcelTemplate {
    private static final Logger log = LoggerFactory.getLogger(ExcelTemplate.class);

    private String fileName;
    private List<Column> columns;
    private IAccreditManager accreditManager;
    private HistoryWriter historyWriter;
    private DataSet dataSet;
    private final DateFormat df1 = new DateFormat("yyyy-MM-dd");
    private final DateFormat df2 = new DateFormat("yyyy-MM-dd HH:mm:ss");
    private int row = 0;
    private final DecimalFormat decimalformat = new DecimalFormat(ApplicationConfig.getPattern());

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    public IAccreditManager getAccreditManager() {
        return accreditManager;
    }

    public void setAccreditManager(IAccreditManager accredit) {
        this.accreditManager = accredit;
    }

    public HistoryWriter getHistoryWriter() {
        return historyWriter;
    }

    public void setHistoryWriter(HistoryWriter historyWriter) {
        this.historyWriter = historyWriter;
    }

    public void output(WritableSheet sheet) throws WriteException {
        boolean changeRowHeight = false;
        // 输出列头
        for (int col = 0; col < columns.size(); col++) {
            Column column = columns.get(col);
            // 若导出模板中包含图片列，则调整行高
            if (column instanceof ImageColumn) {
                changeRowHeight = true;
            }
            Label item = new Label(col, row, column.getName());
            sheet.addCell(item);
        }

        // 输出列数据
        if (dataSet != null) {
            dataSet.first();
            // FIXME: 2021/3/11 目前仅台湾使用负数括号的千分位格式 -1502 显示为 (1,502)
            NumberFormat nf = new NumberFormat(ApplicationConfig.NEGATIVE_PATTERN_TW);
            WritableCellFormat wc = new WritableCellFormat(nf);
            while (dataSet.fetch()) {
                row++;
                for (int col = 0; col < columns.size(); col++) {
                    Column column = columns.get(col);
                    column.setRecord(dataSet.current());
                    // 650像素大约每一行占2.5行
                    if (changeRowHeight) {
                        sheet.setRowView(row, 650, false);
                    }
                    if (sheet.getRows() > 65535)
                        throw new RuntimeException("你导出的数据量过大，超过了Excel的上限，请调整查询条件");
                    writeColumn(sheet, col, row, column, wc);
                }
            }
        }
    }

    protected void writeColumn(WritableSheet sheet, int col, int row, Column column, WritableCellFormat wc)
            throws WriteException {
        if (column instanceof NumberColumn) {
            if (LanguageResource.isLanguageTW()) {
                Label item = new Label(col, row, decimalformat.format(column.getValue()));
                sheet.addCell(item);
            } else {
                jxl.write.Number item = new jxl.write.Number(col, row, (double) column.getValue());
                sheet.addCell(item);
            }
        } else if (column instanceof NumberFormatColumn) {
            jxl.write.Number item;
            if (wc != null) {
                item = new jxl.write.Number(col, row, (double) column.getValue(), wc);
            } else {
                item = new jxl.write.Number(col, row, (double) column.getValue());
            }
            sheet.addCell(item);
        } else if (column instanceof DateColumn) {
            Object value = column.getValue();
            if (value instanceof String) {
                Label item = new Label(col, row, value.toString());
                sheet.addCell(item);
            } else {
                FastDate day = (FastDate) value;
                DateTime item = new DateTime(col, row, day.asBaseDate(), new WritableCellFormat(df1));
                sheet.addCell(item);
            }
        } else if (column instanceof DateTimeColumn) {
            Object value = column.getValue();
            if (value instanceof String) {
                Label item = new Label(col, row, value.toString());
                sheet.addCell(item);
            } else {
                Datetime time = (Datetime) value;
                DateTime item = new DateTime(col, row, time.asBaseDate(), new WritableCellFormat(df2));
                sheet.addCell(item);
            }
        } else if (column instanceof ImageColumn) {
            if (!Utils.isEmpty(column.getValue().toString())) {
                String imageUrl = column.getValue().toString();
                try {
                    // 截取https://ossBucket.ossSite后面的部分
                    if (imageUrl.startsWith("http")) {
                        Optional<String> childUrl = MongoOSS.getChildUrl(imageUrl);
                        if (childUrl.isPresent())
                            imageUrl = childUrl.get();
                    }
                    InputStream inputStream;
                    if (MongoOSS.findByName(imageUrl).isPresent()) {
                        inputStream = MongoOSS.download(imageUrl);
                    } else {
                        OssConnection ossConnection = new OssConnection();
                        // 兼容main分支，后续更新main之后删除，避免发包后影响main
                        String bucket = ossConnection.getBucket();

                        TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET,
                                TimeUnit.MINUTES.toSeconds(5));
                        request.setBucketName(bucket);
                        request.setObjectKey(fileName);

                        Map<String, Object> queryParams = new HashMap<String, Object>();
                        queryParams.put("x-image-process", "image/resize,m_lfit,h_80,w_80/format,png");
                        request.setQueryParams(queryParams);

                        TemporarySignatureResponse resp = ossConnection.getClient().createTemporarySignature(request);
                        URL url = new URL(resp.getSignedUrl());
                        inputStream = url.openStream();
                    }
                    byte[] bytes = new byte[1024];
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int n;
                    while ((n = inputStream.read(bytes)) != -1) {
                        output.write(bytes, 0, n);
                    }
                    WritableImage item = new WritableImage(col, row, 1, 1, output.toByteArray());
                    sheet.addImage(item);
                    inputStream.close();
                    output.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    // 图片解析错误默认输出空白，保证正常导出
                    Label item = new Label(col, row, "");
                    sheet.addCell(item);
                }
            } else {
                Label item = new Label(col, row, "");
                sheet.addCell(item);
            }
        } else {
            Label item = new Label(col, row, column.getValue().toString());
            sheet.addCell(item);
        }
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public DataSet dataSet() {
        return dataSet;
    }

    @Deprecated
    public final DataSet getDataSet() {
        return dataSet();
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }
}
