package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.annotation.Description;

public class FunctionManageTest {

    @Test
    @Description("四则运算")
    public void test() {
        FunctionManage fm = new FunctionManage();
        fm.addFunction(new FunctionMath());
        assertEquals("101", fm.process("math(1+100)"));
        assertEquals("5", fm.process("math(1*2+3)"));
        assertEquals("7", fm.process("math(1+2*3)"));
    }

    @Test
    @Description("条件运行")
    public void test_if() {
        FunctionManage fm = new FunctionManage();
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());
        assertEquals("11", fm.process("if(true,11,2)"));
    }

}
