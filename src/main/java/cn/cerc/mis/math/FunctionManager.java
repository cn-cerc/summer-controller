package cn.cerc.mis.math;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;

public class FunctionManager implements IHandle {
    private static final Logger log = LoggerFactory.getLogger(FunctionManager.class);
    private List<IFunction> funcItems = new ArrayList<>();
    private ISession session;
    private ArrayList<IFunctionNode> items;

    public FunctionManager() {
        super();
    }

    public FunctionManager(IHandle handle) {
        super();
        for (var bean : Application.getContext().getBeansOfType(IFunction.class).values()) {
            if (bean instanceof IHandle item)
                item.setSession(handle.getSession());
            this.addFunction(bean);
        }
    }

    public FunctionManager addFunction(IFunction func) {
        funcItems.add(func);
        return this;
    }

    public List<IFunction> items() {
        return funcItems;
    }

    public Variant parse(String text) {
        log.debug("------process text: {} ------", text);
        this.items = this.createNodes(text);
//        String result = this.process(new FunctionData(text));
//        log.debug("result: {}", result);
        return new Variant(1);
    }

    /**
     * 请改使用 parse
     * 
     * @param text
     * @return
     */
    @Deprecated
    public String process(String text) {
        return parse(text).getString();
    }

    private String process(FunctionData data) {
        String result = "";
        for (IFunction func : funcItems) {
            String name = data.name().toLowerCase();
            if (func.name().equals(name)) {
                result = func.call(this, data.name(), data.param());
                break;
            }
        }
        return result;
    }

    /**
     * 校验是否为内嵌函数调用
     * 
     * @param text 表达式
     * @return 表达式
     */
    public String childProcess(String text) {
        FunctionData one = new FunctionData(text);
        if (!"".equals(one.name()))
            return this.process(one);

        // 下一次循环的位置，默认从头循环
        int nextStart = 0;
        while (true) {
            // 标记是否取到了函数
            int flag = 0;
            // 字母初始位置（角标）
            int letterIndex = -1;
            // 左边括号初始位置（角标）
            int lBracketsIndex = -1;
            for (int i = nextStart; i < text.length(); i++) {
                String ch = text.substring(i, i + 1);
                // 记录第一个字母的位置（角标）
                if (letterIndex == -1 && ch.matches("[a-zA-Z]+"))
                    letterIndex = i;
                if (letterIndex > -1) {
                    // 记录第一个左括号的位置（角标）
                    if (lBracketsIndex == -1 && ch.equals("("))
                        lBracketsIndex = i;
                    if (lBracketsIndex > -1) {
                        if (ch.equals("("))
                            flag++;
                        if (ch.equals(")"))
                            flag--;
                        // flag大于1，则说明连续出现了两个(,是函数内套了函数，退出循环，从一个左括号之后开始重新循环
                        if (flag > 1) {
                            nextStart = lBracketsIndex + 1;
                            break;
                        }
                        // flag等于0，则说明取到了函数
                        if (flag == 0) {
                            String data = text.substring(letterIndex, i + 1);
                            // 将函数执行之后，替换掉原来此函数的位置，再重新循环，查找下一个函数
                            text = text.substring(0, letterIndex) + this.childProcess(data)
                                    + text.substring(i + 1, text.length());
                            nextStart = 0;
                            break;
                        }
                    }
                }
            }
            // 没有字母或者没有括号，则退出
            if (letterIndex == -1 || lBracketsIndex == -1)
                break;
        }
        return text;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    /**
     * 
     * @param templateText
     * @return 根据模版创建 ssr 节点
     */
    public ArrayList<IFunctionNode> createNodes(String value) {
        var text = value;
        var items = new ArrayList<IFunctionNode>();
        if (value.indexOf('(') == -1) {
            items.add(new FunctionValue(this, value));
            return items;
        }
        var temp = text;
        for (var func : this.funcItems) {
            var name = func.name() + "(";
            var start = temp.indexOf(name);
            if (start > 0) {
                var find = 1;
                var s2 = temp.substring(start, temp.length());
                for (var i = name.length(); i <= s2.length(); i++) {
                    var flag = s2.charAt(i);
                    if ('(' == flag)
                        find++;
                    if (')' == flag)
                        find--;
                    if (find == 0) {
                        var funcText = temp.substring(start, start + i + 1);
                        if (start > 0) {
                            items.add(new FunctionValue(this, temp.substring(0, start)));
                        }
                        items.add(new FunctionValue(this, funcText));
                        if (start + i + 1 < temp.length())
                            items.add(new FunctionValue(this, temp.substring(start + i + 1, temp.length())));
                        break;
                    }
                }
                if (find != 0)
                    throw new RuntimeException("error text: " + text);
                break;
            }
        }
        System.out.println("++++++++++: " + value);
        for (var item : items)
            System.out.println(item.text());
        return items;
    }

    public static void main(String[] args) {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionIf());
        fm.parse("1+if(a,b,c())+2");
    }
}
