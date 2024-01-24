package cn.cerc.mis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.ServiceException;

public class CustomEntityServiceTest {
    private static final Logger log = LoggerFactory.getLogger(CustomEntityServiceTest.class);

    @Test
    public void test_call() {
        var svr = new StubEntityService();
        var dataIn = new DataSet();
        dataIn.head().setValue("tbNo_", "OD20010001");
        try {
            var dataOut = svr.execute(Handle.getStub(), dataIn);
            assertEquals("{\"state\":1}", dataOut.json());
        } catch (ServiceException | DataException e) {
            log.error(e.getMessage(), e);
        }
    }

}
