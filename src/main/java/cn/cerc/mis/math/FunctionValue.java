package cn.cerc.mis.math;

import java.util.ArrayList;
import java.util.List;

public class FunctionValue implements IFunctionNode {
    public List<IFunctionNode> items = new ArrayList<>();
    private String text;
    private String name;
    private String param;
    private boolean onlyText;

    public FunctionValue(FunctionManager manager, String text) {
        this.text = text;
        var v1 = text.indexOf('(');
        if (v1 > -1 && text.endsWith(")")) {
            this.name = text.substring(0, v1);
            this.param = text.substring(v1 + 1, text.length() - 1);
            this.items = manager.createNodes(param);
        } else {
            this.onlyText = true;
        }
    }

    @Override
    public String text() {
        return text;
    }

    public int size() {
        return items.size();
    }

    public IFunctionNode get(int index) {
        return items.get(index);
    }

    public String name() {
        return name;
    }

    public String param() {
        return param;
    }
}
