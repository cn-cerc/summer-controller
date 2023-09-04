package cn.cerc.mis.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FunctionIf implements IFunction {
    private static final Logger log = LoggerFactory.getLogger(FunctionIf.class);

    @Override
    public String name() {
        return "if";
    }

    @Override
    public String description() {
        return "IF运算，调用范例：if(a1(),a2(200),b1(1...100))";
    }

    @Override
    public String process(FunctionManager manage, String text) {
        if (!text.contains(",")) {
            log.error("无法解析：{}", text);
            return text;
        }
        var args = text.split(",");
        if (args.length != 3) {
            log.error("无法解析：{}", text);
            return text;
        }
        String ifResult = args[0];
        var s1 = args[1];
        var s2 = args[2];
        //
        if ("true".equals(ifResult))
            return s1;
        else if (ifResult.split("==").length == 2) {
            var v1 = ifResult.split("==")[0];
            var v2 = ifResult.split("==")[1];
            return v1.compareTo(v2) == 0 ? s1.trim() : s2.trim();
        } else
            return s2;
    }

}
