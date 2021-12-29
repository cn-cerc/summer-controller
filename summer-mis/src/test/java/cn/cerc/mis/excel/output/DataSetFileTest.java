package cn.cerc.mis.excel.output;

import jxl.write.WriteException;
import org.junit.Ignore;
import org.junit.Test;

import cn.cerc.db.core.DataSet;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class DataSetFileTest {

    @Test
    @Ignore(value = "测试文件建立，仅在本地执行")
    public void testExecute() throws WriteException, IOException {
        DataSet ds = new DataSet();
        ds.append();
        ds.setValue("code", "code1");
        ds.append();
        ds.setValue("code", "code2");

        String fileName = "d:\\tempfile.xls";
        new DataSetFile(ds).save(fileName);
        File file = new File(fileName);
        assertTrue("文件建立失败！", file.exists());
    }
}
