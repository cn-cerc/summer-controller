package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.annotation.Description;

public class FunctionManagerTest {

    @Test
    @Description("四则运算")
    public void test() {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionMath());
        assertEquals("101", fm.parse("math(1+100)").getString());
        assertEquals("5", fm.parse("math(1*2+3)").getString());
        assertEquals("7", fm.parse("math(1+2*3)").getString());
    }

    @Test
    @Description("四则运算")
    public void test_2() {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());
        fm.addFunction(new IFunction() {

            @Override
            public String name() {
                return "me";
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String process(FunctionManager manage, String text) {
                return "false";
            }
            
        });
        assertEquals(101, fm.parse("math(1+if(me(), 10, 100))").getInt());
    }

}
