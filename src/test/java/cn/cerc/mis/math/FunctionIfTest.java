package cn.cerc.mis.math;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class FunctionIfTest {

//    @Test
    public void test() {
        FunctionIf ff = new FunctionIf();
        assertEquals("11", ff.process(null, "true,11,2"));
    }

    @Test
    public void test_ifAndMath() {
        FunctionManage manage = new FunctionManage();
        manage.addFunction(new FunctionMath());
        manage.addFunction(new FunctionIf());
        assertEquals("0", manage.parse("if(true,0,math(0/0*100))").getString());
        assertEquals("7", manage.parse("if(false,0,math(1+3*2))").getString());
        System.out.println(
                manage.parse("if(true,math(1+if(true,1,0)*2*(1+3)),if(true,math(1+1),math((1+2)*3)))").getString());
    }

    private static Set<Character> opreaSet = Set.of('+', '-', '*', '/', ',');

    private static LinkedList<String> methods = new LinkedList<>();
    private static List<Data> datas = new ArrayList<>();

    public record Data(String method, String content) {
    }

    public static int parse(String cal) {
        int index = 0;
        int opreaIndex = 0;
        while (index < cal.length()) {
            if (opreaSet.contains(cal.charAt(index)))
                opreaIndex = index;
            if (cal.charAt(index) == '(') {
                String method = cal.substring(opreaIndex == 0 ? 0 : opreaIndex + 1, index);
                methods.push(method);
                index += parse(cal.substring(index + 1)) + 1;
            } else if (index < cal.length()) {
                if (cal.charAt(index) == ')') {
                    String content = cal.substring(0, index);
                    String method = methods.poll();
                    datas.add(new Data(method, content));
                    return index;
                }
            }
            index++;
        }
        return 0;
    }

    public static void main(String[] args) {
        String cal = "if(true,math(1+if(true,1,0)*2*(1+3)),if(true,a(),math(1+1),math((1+2)*3)))";
        parse(cal);
        System.out.println(datas.size());
        for (int i = 0; i < datas.size(); i++) {
            Data data = datas.get(i);
            System.out.println("%s %s".formatted(data.method, data.content));
        }
    }

}
