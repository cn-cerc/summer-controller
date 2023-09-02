package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FunctionValueTest {

    @Test
    public void test() {
        FunctionData fv = new FunctionData(null, "a(2)");
        assertEquals("a", fv.name());
        assertEquals("2", fv.param());
        assertEquals("a(2)", fv.text());
    }

}
