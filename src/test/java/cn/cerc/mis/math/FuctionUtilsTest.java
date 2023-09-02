package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class FuctionUtilsTest {

//    @Test
    public void test_1() {
        FunctionManager fm = new FunctionManager();
        var items = fm.createNodes("a+b()");
        assertEquals(2, items.size());
        assertEquals("a+", items.get(0).text());
        assertEquals("b()", items.get(1).text());
    }

    @Test
    public void test_2() {
        FunctionManager fm = new FunctionManager();
        var items = fm.createNodes("c(2)+1");
        assertEquals(2, items.size());
        assertEquals("c(2)", items.get(0).text());
        assertEquals("+1", items.get(1).text());
    }

//    @Test
    public void test() {
        FunctionManager fm = new FunctionManager();
        ArrayList<IFunctionNode> nodes = fm.createNodes("a+b(a(c(2)+1))+3");
        assertEquals(3, nodes.size());
        assertEquals("a+", nodes.get(0).text());
        assertEquals("b(a(c(2)+1))", nodes.get(1).text());
        assertEquals("+3", nodes.get(2).text());
        //
        FunctionNode node1 = (FunctionNode) nodes.get(1);
        assertEquals("a(c(2)+1)", node1.get(0).text());
        assertEquals(1, node1.size());
        //
        FunctionNode node2 = (FunctionNode) node1.get(0);
        assertEquals(1, node2.size());
        assertEquals("c(2)+1", node2.get(0));
        //
        FunctionNode node3 = (FunctionNode) node2.get(0);
        assertEquals(2, node3.size());
        assertEquals("c(2)", node3.get(0));
        assertEquals("+1", node3.get(1));
    }

}
