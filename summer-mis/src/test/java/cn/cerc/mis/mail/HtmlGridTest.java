package cn.cerc.mis.mail;

import org.junit.Test;

import cn.cerc.db.core.DataSet;

import static org.junit.Assert.assertTrue;

public class HtmlGridTest {
    @Test
    public void test_getDataSet() {
        DataSet ds = new DataSet();
        for (int i = 1; i < 3; i++)
            ds.append().setValue("Code", "C00" + i).setValue("Name", "N00" + i);
        String str = HtmlGrid.getDataSet(ds);
        assertTrue(!"".equals(str));
    }
}
