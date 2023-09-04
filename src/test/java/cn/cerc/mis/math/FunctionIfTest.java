package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.cerc.db.core.DataRow;

public class FunctionIfTest {

    @Test
    public void test_1() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "true,11,2"));
    }

    @Test
    public void test_2() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "1==1,11,2"));
    }

    @Test
    public void test_3() {
        FunctionIf ff = new FunctionIf();
        assertEquals("2", ff.process(null, "1!=1,11,2"));
    }

    @Test
    public void test_4() {
        FunctionIf ff = new FunctionIf();
        assertEquals("2", ff.process(null, "1>2,11,2"));
    }

    @Test
    public void test_5() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "1>=1,11,2"));
    }

    @Test
    public void test_6() {
        FunctionIf ff = new FunctionIf();
        assertEquals("2", ff.process(null, "1<1,11,2"));
    }

    @Test
    public void test_7() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "1<=1,11,2"));
    }

    @Test
    public void test_8() {
        FunctionManager manager = new FunctionManager();
        manager.addFunction(new FunctionIf());
        manager.addFunction(new FunctionField(DataRow.of("value_", "")));
        assertEquals(1, manager.parse("if(value_==null,1,0)").getInt());
    }

    @Test
    public void test_9() {
        FunctionManager manager = new FunctionManager();
        manager.addFunction(new FunctionIf());
        manager.addFunction(new FunctionField(DataRow.of("value_", "")));
        assertEquals(0, manager.parse("if(value_!=null,1,0)").getInt());
    }

}
