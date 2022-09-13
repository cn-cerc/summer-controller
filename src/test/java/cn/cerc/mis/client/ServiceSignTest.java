package cn.cerc.mis.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import cn.cerc.db.Alias;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Variant;

public class ServiceSignTest {

    public interface TestHead {
        @Alias("tbNo_")
        String tbNo();
    }

    public interface TestBody {
        Variant code();

        int num();
    }

    public interface SvrDeptImpl extends ServiceSignImpl {
        TestHead head();

        List<TestBody> body();
    }

    @Test
    public void test() {
        SvrDeptImpl impl = ServiceSign.build("SvrDept", SvrDeptImpl.class);

        // 跳过远程服务，直接赋值，便于测试，实际作业时，请改为 impl.call(handle)
        ServiceSign sign = impl.sign();
        DataSet dataOut = sign.dataOut();
        dataOut.setState(1);
        dataOut.head().setValue("tbNo_", "OD002");
        dataOut.append().setValue("code", 1).setValue("num", 3);
        dataOut.append().setValue("code", 2).setValue("num", 5);

        // 判断结果是否正确
        assertTrue(sign.isOk());
        assertEquals("OD002", impl.head().tbNo());

        int sum = 0;
        for (TestBody item : impl.body())
            sum += item.num();
        assertEquals(8, sum);
    }

}
