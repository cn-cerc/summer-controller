package cn.cerc.mis.math;

import java.util.List;

public class FunctionExpression extends FunctionNode {
    private String name;
    private String param;
    private IFunction function;
    private List<FunctionNode> items;
    private String decodeValue = null;

    public FunctionExpression(FunctionManager manager, String text) {
        super(manager, text);
        var site = text.indexOf('(');
        if (site > -1 && text.endsWith(")")) {
            if (site == 0) {
                this.name = "";
                this.param = text.substring(1, text.length() - 1);
            } else {
                this.name = text.substring(0, site);
                this.param = text.substring(site + 1, text.length() - 1);
            }
            this.items = manager.createNodes(this.param);
        } else {
            throw new RuntimeException("错误的函数：" + text);
        }
    }

    public String value() {
        if (decodeValue == null) {
            var sb = new StringBuffer();
            for (var item : this.items)
                sb.append(item.value());
            if (function != null)
                decodeValue = function.process(this.manager(), sb.toString());
            else
                decodeValue = sb.toString();
        }
        return decodeValue;
    }

    public String param() {
        return param;
    }

    public void function(IFunction function) {
        this.function = function;
    }

    public String name() {
        return this.name;
    }

}
