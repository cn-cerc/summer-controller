package cn.cerc.mis.math;

public class FunctionData {
    private String name = "";
    private String param = "";

    public FunctionData(String text) {
        super();

        int start = text.indexOf("(");
        int end = text.lastIndexOf(")");
        if (start > -1 && end == text.length() - 1) {
            int flag = 0;
            for (int i = start + 1; i < end; i++) {
                if (text.substring(i, i + 1).equals("("))
                    flag++;
                if (text.substring(i, i + 1).equals(")"))
                    flag--;
                if (flag < 0)
                    return;
            }

            this.name = text.substring(0, start);
            this.param = text.substring(start + 1, end);
        }
    }

    public String name() {
        return this.name;
    }

    public String param() {
        return this.param;
    }

}
