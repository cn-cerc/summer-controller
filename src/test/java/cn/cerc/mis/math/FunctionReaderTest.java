package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class FunctionReaderTest {

	@Test
	public void test_1() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(0, fr.parse(null));
		assertEquals(0, fr.parse(""));
	}

	@Test
	public void test_2() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(0, fr.parse("a+b"));
		assertEquals(1, fr.parse("a+b()"));
		assertEquals(1, fr.parse("a(b())"));
		assertEquals(1, fr.parse("1+a(b())"));
		assertEquals("[a+, b, a+, 1+]", items1.toString());
		assertEquals("[b(), a(b()), a(b())]", items2.toString());
	}

	@Test
	public void test_3() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(1, fr.parse("1+a(b())+2"));
		assertEquals(1, fr.parse("a(b())+2"));
		assertEquals("[1+, +2, +2]", items1.toString());
		assertEquals("[a(b()), a(b())]", items2.toString());
	}

	@Test
	public void test_4() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(2, fr.parse("a()+b()"));
		assertEquals(2, fr.parse("1+a()+b()"));
		assertEquals(2, fr.parse("1+a()+b()+2"));
		assertEquals("[+, 1+, +, 1+, +, +2]", items1.toString());
		assertEquals("[a(), b(), a(), b(), a(), b()]", items2.toString());
	}

	@Test
	public void test_5() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(2, fr.parse("a()+3+b()+2"));
		assertEquals(2, fr.parse("a(b())+c()"));
		assertEquals(2, fr.parse("a(b()+d())+c()"));
		assertEquals("[+3+, +2, +, +]", items1.toString());
		assertEquals("[a(), b(), a(b()), c(), a(b()+d()), c()]", items2.toString());
	}

	@Test
	public void test_6() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader();
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(1, fr.parse("a+b+c()"));
		assertEquals("[a+, b+]", items1.toString());
		assertEquals("[c()]", items2.toString());
	}

	@Test
	public void test_7() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader(true);
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(1, fr.parse("a+b+c()"));
		assertEquals("[a, +, b, +]", items1.toString());
		assertEquals("[c()]", items2.toString());
	}

	@Test
	public void test_8() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader(true);
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(0, fr.parse("(1+2)*3"));
		assertEquals("[(1+2), *3]", items1.toString());
		assertEquals("[]", items2.toString());
	}

	@Test
	public void test_9() {
		var items1 = new ArrayList<String>();
		var items2 = new ArrayList<String>();
		FunctionReader fr = new FunctionReader(true);
		fr.onText(text -> items1.add(text));
		fr.onFunction(text -> items2.add(text));
		assertEquals(1, fr.parse("(1+2)*3/(5-c2())"));
		assertEquals("[(1+2), *3/(5-, )]", items1.toString());
		assertEquals("[c2()]", items2.toString());
	}
	
	@Test
	public void test_10() {
	    var items1 = new ArrayList<String>();
        var items2 = new ArrayList<String>();
        FunctionReader fr = new FunctionReader(true);
        fr.onText(text -> items1.add(text));
        fr.onFunction(text -> items2.add(text));
        assertEquals(0, fr.parse("true,11,3"));
        assertEquals("[true, ,11,3]", items1.toString());
        assertEquals("[]", items2.toString());
	}
}
