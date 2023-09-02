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
    private ArrayList<IFunctionNode> nodes;

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
            items.add(new FunctionData(this, value));
            return items;
        }
        var temp = text;
        IFunction selected = null;
        var minStart = -1;
        for (var func : this.funcItems) {
            var name = func.name() + "(";
            var start = temp.indexOf(name);
            if (start > -1) {
                if (selected == null || start < minStart) {
                    selected = func;
                    minStart = start;
                }
            }
        }
        // 先找出函数值最小的一个
        if (selected != null) {
            var name = selected.name() + "(";
            var find = 1;
            var s2 = temp.substring(minStart, temp.length());
            for (var i = name.length(); i < s2.length(); i++) {
                var flag = s2.charAt(i);
                if ('(' == flag)
                    find++;
                else if (')' == flag) {
                    find--;
                    if (find == 0) {
                        var funcText = temp.substring(minStart, minStart + i + 1);
                        if (minStart > 0) {
                            items.add(new FunctionData(this, temp.substring(0, minStart)));
                        }
                        items.add(new FunctionData(this, funcText));
                        if (minStart + i + 1 < temp.length())
                            items.add(new FunctionData(this, temp.substring(minStart + i + 1, temp.length())));
                        break;
                    }
                }
            }
            if (find != 0)
                throw new RuntimeException("公式错误，左右括号不配套: " + text);
        }
        return items;
    }

    public Variant parse(String text) {
        this.nodes = this.createNodes(text); // a() + b(d() + e()) + c()
        System.out.println("level 0: " + nodes.size());
        for (var item : nodes)
            System.out.println(item.text());
        for (var item : nodes)
            item.echo(1);
        return new Variant(1);
    }

    public ArrayList<IFunctionNode> nodes() {
        return this.nodes;
    }

    public static void main(String[] args) {
        FunctionManager fm = new FunctionManager();
        fm.addFunction(new FunctionIf());
        fm.addFunction(new FunctionMath());
//        fm.parse("if(true,math(1+if(true,1,0)*2*(1+3)),if(true,a(),math(1+1),math((1+2)*3)))");
        fm.parse("math() + if(math() + math()) + math()");
//        fm.parse(" + if(math() + math()) + math()");
    }

    public String childProcess(String s1) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object process(String string) {
        // TODO Auto-generated method stub
        return null;
    }
}
