package cn.cerc.mis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;

public class CustomEntityServiceTest {

    @Test
    public void test_metaHead() {
        var svr = new StubEntityService();
        assertEquals("""
                {"body":[["code","name","remark","width"],["tbNo_","单号","",20]]}
                """.trim(), svr.getMetaHeadIn(Handle.getStub(), null).json());
    }

    @Test
    public void test_metaBody() {
        var svr = new StubEntityService();
        assertEquals("""
                {"body":[["code","name","remark","width"],["tbNo_","单号","",20],["it_","单序","",4]]}
                """.trim(), svr.getMetaBodyIn(Handle.getStub(), null).json());
    }

    @Test
    public void test_call() {
        var svr = new StubEntityService();
        var dataIn = new DataSet();
        dataIn.head().setValue("tbNo_", "OD20010001");
        try {
            var dataOut = svr.execute(Handle.getStub(), dataIn);
            assertEquals("{\"state\":1}", dataOut.json());
        } catch (DataValidateException e) {
            e.printStackTrace();
        }
    }

}
