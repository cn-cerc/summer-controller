package cn.cerc.mis.excel.input;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;

public class ImportExcelFile {
    private HttpServletRequest request;
    private List<FileItem> uploadFiles;
    private DataSet dataSet;

    public int init() throws UnsupportedEncodingException {
        dataSet = new DataSet();
        // 处理文件上传
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // 设置最大缓存
        factory.setSizeThreshold(5 * 1024);
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            uploadFiles = upload.parseRequest(request);
        } catch (FileUploadException e) {
            // log.error(e.getMessage(), e);
            uploadFiles = null;
            return 0;
        }
        if (uploadFiles != null) {
            for (int i = 0; i < uploadFiles.size(); i++) {
                FileItem fileItem = uploadFiles.get(i);
                if (fileItem.isFormField()) {
                    // 普通数据
                    String val = new String(fileItem.getString().getBytes(StandardCharsets.ISO_8859_1),
                            StandardCharsets.UTF_8);
                    dataSet.head().setValue(fileItem.getFieldName(), val);
                } else {
                    // 文件数据
                    if (fileItem.getSize() > 0) {
                        DataRow rs = dataSet.append().current();
                        rs.setValue("_FieldNo", i);
                        rs.setValue("_FieldName", fileItem.getFieldName());
                        rs.setValue("_ContentType", fileItem.getContentType());
                        rs.setValue("_FileName", fileItem.getName());
                        rs.setValue("_FileSize", fileItem.getSize());
                    }
                }
            }
        }
        dataSet.first();
        return dataSet.size();
    }

    public FileItem getFile(DataRow record) {
        int fileNo = record.getInt("_FieldNo");
        return uploadFiles.get(fileNo);
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public ImportExcelFile setRequest(HttpServletRequest request) {
        this.request = request;
        return this;
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
