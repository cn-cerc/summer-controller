package cn.cerc.mis.math;

public class FunctionNode {
    private String text;
    private FunctionManager manager;
    private IFunction function;

    public FunctionNode(FunctionManager manager, String text) {
        this.manager = manager;
        this.text = text;
    }

    public String text() {
        return text;
    }

    public FunctionManager manager() {
        return manager;
    }

    public String value() {
        if (function != null)
            return function.process(manager, text);
        else
            return text != null ? this.text : "";
    }

    public void function(IFunction function) {
        this.function = function;
    }

}
