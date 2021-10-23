package cn.cerc.mis.security;

import java.util.ArrayList;
import java.util.List;

public class OperatorData {
    private String code;
    private List<String> items = new ArrayList<>();

    public OperatorData(String code) {
        super();
        this.code = code;
    }

    public final String getCode() {
        return code;
    }

    public final void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.code);
        if (this.items.size() > 0) {
            sb.append("[");
            for (int i = 0; i < items.size(); i++) {
                sb.append(items.get(i));
                if (i < (items.size() - 1))
                    sb.append(",");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public OperatorData add(String item) {
        if (this.items.indexOf(item) == -1)
            this.items.add(item);
        return this;
    }

    public static void main(String[] args) {
        OperatorData item = new OperatorData("abc");
        item.add("a").add("b");
        System.out.println(item);
    }
}
