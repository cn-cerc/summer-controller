package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.annotation.Description;

public class FunctionManagerTest1 {

    @Test
    @Description("条件运行")
    public void test_if() {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());
        assertEquals("11", fm.parse("if(true,11,3)").getString());
    }

    @Test
    @Description("条件运行")
    public void test_if2() {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());
        assertEquals("1", fm.parse("if(false,11,if(true,1,2))").getString());
    }

}
