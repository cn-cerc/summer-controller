package cn.cerc.mis.client;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import cn.cerc.db.Alias;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.Variant;

public class ServiceSignTest {

    public interface TestHead {
        @Alias("tbNo_")
        Variant tbNo();
    }

    public interface TestBody {
        Variant code();

        Variant name();
    }

    public interface SvrDeptImpl extends ServiceProxy {
        TestHead head();

        List<TestBody> body();
    }

    @Test
    @Ignore
    public void test() {
        SvrDeptImpl impl = ServiceSign.build("SvrDept", SvrDeptImpl.class);
        if (impl.call(null, DataRow.of("a", 1)).isOk()) {
            System.out.println(impl.head().tbNo().getString());
            impl.body().forEach(item -> System.out.println(item.code().getString()));
        }
    }

}
