package cn.cerc.mis.math;

import java.util.ArrayList;
import java.util.List;

public class FunctionData implements IFunctionNode {
    public List<IFunctionNode> items = new ArrayList<>();
    private String text;
    private String name;
    private String param;
    private boolean onlyText;

    public FunctionData(FunctionManager manager, String text) {
        this.text = text;
        var v1 = text.indexOf('(');
        if (v1 > -1) {
            this.items = manager.createNodes(text);
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

    @Override
    public void echo(int level) {
        if (items.size() > 0) {
            System.out.println("level: " + level + ", name: " + this.name);
            for (var item : this.items)
                System.out.println(item.text());
            for (var item : this.items)
                item.echo(level + 1);
        }
    }
}
