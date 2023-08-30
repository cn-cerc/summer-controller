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
        return "IF运算，调用范例：if(a1()?a2(200):b1(1...100))";
    }

    @Override
    public String process(FunctionManage manage, String text) {
        if (!text.contains("?") || !text.contains(":")) {
            log.error("无法解析：{}", text);
            return text;
        }
        String ifResult = text.split("\\?")[0];
        // 取结果
        var temp = text.split("\\?")[1];
        var s1 = temp.split(":")[0].trim();
        var s2 = temp.split(":")[1].trim();
        if (manage != null) {
            ifResult = manage.childProcess(ifResult);
            if ("true".equals(ifResult)) {
                return manage.childProcess(s1);
            } else {
                return manage.childProcess(s2);
            }
        } else {
            if ("true".equals(ifResult)) {
                return s1;
            } else {
                return s2;
            }
        }
    }

}
