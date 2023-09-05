package cn.cerc.mis.math;

import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.Utils;

@Component
public class FunctionIf implements IFunction {
    private static final Logger log = LoggerFactory.getLogger(FunctionIf.class);

    private static final BiFunction<String, String, Boolean> EQUALS_FUNC = (v1, v2) -> {
        if ("null".equals(v1))
            return Utils.isEmpty(v2);
        else if ("null".equals(v2))
            return Utils.isEmpty(v1);
        return v1.equals(v2);
    };
    private static final BiFunction<String, String, Boolean> NO_EQUALS_FUNC = (v1, v2) -> !EQUALS_FUNC.apply(v1, v2);

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
        CompareResult compare = null;
        if ("true".equals(ifResult))
            return s1;
        else if ((compare = compare(ifResult, "==", EQUALS_FUNC)).match()
                || (compare = compare(ifResult, "!=", NO_EQUALS_FUNC)).match()
                || (compare = compare(ifResult, ">=", (v1, v2) -> v1.compareTo(v2) >= 0)).match()
                || (compare = compare(ifResult, "<=", (v1, v2) -> v1.compareTo(v2) <= 0)).match()
                || (compare = compare(ifResult, ">", (v1, v2) -> v1.compareTo(v2) > 0)).match()
                || (compare = compare(ifResult, "<", (v1, v2) -> v1.compareTo(v2) < 0)).match()) {
            return compare.result() ? s1.trim() : s2.trim();
        } else
            return s2;
    }

    private CompareResult compare(String ifResult, String opera, BiFunction<String, String, Boolean> compareFunc) {
        String[] splitArr = ifResult.split(opera);
        if (splitArr == null || splitArr.length < 2)
            return new CompareResult(false, false);
        return new CompareResult(splitArr.length == 2, compareFunc.apply(splitArr[0], splitArr[1]));
    }

    private record CompareResult(boolean match, boolean result) {

    }

}
