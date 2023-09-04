package cn.cerc.mis.math;

import java.util.ArrayList;
import java.util.List;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;

public class FunctionManager implements IHandle {
//    private static final Logger log = LoggerFactory.getLogger(FunctionManager.class);
    private List<IFunction> funcItems = new ArrayList<>();
    private List<FunctionNode> items;
    private ISession session;
    private FunctionReader reader = new FunctionReader();

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
        if (text.startsWith("="))
            this.items = createNodes("math(" + text.substring(1, text.length()) + ")");
        else
            this.items = createNodes(text);
        StringBuffer sb = new StringBuffer();
        for (var item : items)
            sb.append(item.value());
        return new Variant(sb.toString());
    }

    protected List<FunctionNode> createNodes(String text) {
        var items = new ArrayList<FunctionNode>();
        reader.onText(value -> {
            var item = new FunctionNode(this, value);
            items.add(item);
            for (var func : this.funcItems) {
                if (func.isName(value)) {
                    item.function(func);
                    break;
                }
            }

        });
        reader.onFunction(value -> {
            var item = new FunctionExpression(this, value);
            items.add(item);
            for (var func : this.funcItems) {
                if (func.isName(item.name())) {
                    item.function(func);
                    break;
                }
            }
        });
        reader.parse(text);
        return items;
    }

    @Deprecated
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
    @Deprecated
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

    public FunctionReader reader() {
        return reader;
    }

}
