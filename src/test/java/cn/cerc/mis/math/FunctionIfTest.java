package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FunctionIfTest {

    @Test
    public void test() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "true,11,2"));
    }

    @Test
    public void test_ifAndMath() {
        FunctionManage manage = new FunctionManage();
        manage.addFunction(new FunctionIf());
        manage.addFunction(new FunctionMath());
        System.out.println(manage.parse("if(true,0,math(0/0*100))").getString());
    }

}
