package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FunctionIfTest {

    @Test
    public void test() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "true?11:2"));
    }

}
