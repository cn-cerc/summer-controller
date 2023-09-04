package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.annotation.Description;

import cn.cerc.db.core.DataRow;

public class FunctionManagerTest2 {

    @Test
    @Description("处理计算字段")
    public void test() {
        FunctionManager fm = new FunctionManager();
        fm.reader().setLetterOption(true);
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());

        DataRow row = DataRow.of("price", "2", "num_", "3", "rate_", 1);
        fm.addFunction(new FunctionField(row));
        assertEquals(6, fm.parse("=price*num_*rate_").getInt());
    }

    @Test
    @Description("处理计算字段")
    public void test_1() {
        FunctionManager fm = new FunctionManager();
        fm.reader().setLetterOption(true);
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());

        DataRow row = DataRow.of("price", "2", "num_", "3", "rate_", 0);
        fm.addFunction(new FunctionField(row));
        assertEquals(6, fm.parse("=price*num_*if(rate_==0,1,rate_)").getInt());
    }

    @Test
    @Description("处理计算字段")
    public void test_2() {
        FunctionManager fm = new FunctionManager();
        fm.reader().setLetterOption(true);
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());

        DataRow row = DataRow.of("price", "2", "num_", "3", "rate_", 1);
        fm.addFunction(new FunctionField(row));
        assertEquals(6, fm.parse("=price*num_*if(rate_==0,1,rate_)").getInt());
    }

    @Test
    @Description("处理计算字段")
    public void test_3() {
        FunctionManager fm = new FunctionManager();
        fm.reader().setLetterOption(true);
        fm.addFunction(new FunctionMath());
        fm.addFunction(new FunctionIf());

        DataRow row = DataRow.of("price", "2", "num_", "3", "rate_", 2);
        fm.addFunction(new FunctionField(row));
        assertEquals(12, fm.parse("=price*num_*if(rate_==0,1,rate_)").getInt());
    }

}
