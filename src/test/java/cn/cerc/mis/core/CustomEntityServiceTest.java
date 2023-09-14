package cn.cerc.mis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.ServiceException;

public class CustomEntityServiceTest {

    @Test
    public void test_call() {
        var svr = new StubEntityService();
        var dataIn = new DataSet();
        dataIn.head().setValue("tbNo_", "OD20010001");
        try {
            var dataOut = svr.execute(Handle.getStub(), dataIn);
            assertEquals("{\"state\":1}", dataOut.json());
        } catch (ServiceException | DataException e) {
            e.printStackTrace();
        }
    }

}
