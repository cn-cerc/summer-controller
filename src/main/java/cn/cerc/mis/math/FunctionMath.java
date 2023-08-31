package cn.cerc.mis.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.Utils;

@Component
public class FunctionMath implements IFunction {
    private static final Logger log = LoggerFactory.getLogger(FunctionMath.class);

    @Override
    public String name() {
        return "math";
    }

    @Override
    public String description() {
        return "加减乘除运算，调用实例：math(a2() + (b1() * 30) - a1() / 2)";
    }

    @Override
    public String process(FunctionManager manage, String text) {
        String result = text;
        try {
            result = Utils.formatFloat("#.##", MathUtil.arithmetic(text.replaceAll(" ", "")).doubleValue());
        } catch (Exception e) {
            log.error("函数表达式 {} 计算异常：{}", text, e.getMessage());
        }
        return result;
    }

}
