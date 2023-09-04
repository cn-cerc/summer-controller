package cn.cerc.mis.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算器工具类
 */
public class MathUtil {

    /**
     * 最小计数单位
     */
    private static String minExp = "^((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\]))[\\+\\-\\*\\/]((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\]))$";
    /**
     * 不带括号的运算
     */
    private static String noParentheses = "^[^\\(\\)]+$";
    /**
     * 匹配乘法或者除法
     */
    private static String priorOperatorExp = "(((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\]))[\\*\\/]((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\])))";
    /**
     * 匹配加法和减法
     */
    private static String operatorExp = "(((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\]))[\\+\\-]((\\d+(\\.\\d+)?)|(\\[\\-\\d+(\\.\\d+)?\\])))";
    /**
     * 匹配只带一个括号的
     */
    private static String minParentheses = "\\([^\\(\\)]+\\)";
    /**
     * 匹配只有半边括号的
     */
    private static String baseParentheses = "^\\(+[^\\)]+$|^[^\\(]+\\)+$";
    /**
     * 匹配纯数字
     */
    private static String numberString = "\\d+\\.\\d+|\\d+";

    public static void main(String[] args) {
        System.out.println(arithmetic("-(-1000)+(-222)+(555)"));
    }

    public static BigDecimal arithmetic(String exp) {
        String result = parseExp(exp).replaceAll("[\\[\\]]", "");
        return new BigDecimal(result);
    }

    /**
     * 解析计算四则运算表达式，例：2+((3+4)*2-22)/2*3
     * 
     * @param expression 表达式
     * @return 结果
     */
    private static String parseExp(String expression) {
        // 半边括号 直接抛异常
        if (expression.matches(baseParentheses)) {
            throw new RuntimeException("计算公式存在错误，只有半边括号！");
        }
        // 直接是数字不计算
        if (expression.matches(numberString)) {
            return expression;
        }
        if (expression.startsWith("-") || expression.startsWith("+"))
            expression = "0" + expression;
        expression = expression.replace("+-", "+0-");
        expression = expression.replace("--", "-0-");
        expression = expression.replace("++", "+0+");
        expression = expression.replace("-+", "-0+");
        // 方法进入 先替换空格，在去除运算两边的()号
        expression = expression.replaceAll("\\s+", "").replaceAll("^\\(([^\\(\\)]+)\\)$", "$1");

        // 最小表达式计算
        if (expression.matches(minExp)) {
            String result = calculate(expression);
            return Double.parseDouble(result) >= 0 ? result : "[" + result + "]";
        }
        // 计算不带括号的四则运算
        if (expression.matches(noParentheses)) {
            Pattern patt = Pattern.compile(priorOperatorExp);
            Matcher mat = patt.matcher(expression);
            if (mat.find()) {
                String tempMinExp = mat.group();
                expression = expression.replaceFirst(priorOperatorExp, parseExp(tempMinExp));
            } else {
                patt = Pattern.compile(operatorExp);
                mat = patt.matcher(expression);
                if (mat.find()) {
                    String tempMinExp = mat.group();
                    expression = expression.replaceFirst(operatorExp, parseExp(tempMinExp));
                }
            }
            return parseExp(expression);
        }

        // 计算带括号的四则运算
        Pattern patt = Pattern.compile(minParentheses);
        Matcher mat = patt.matcher(expression);
        if (mat.find()) {
            String tempMinExp = mat.group();
            expression = expression.replaceFirst(minParentheses, parseExp(tempMinExp));
        }
        return parseExp(expression);
    }

    /**
     * 计算最小单位四则运算表达式（两个数字）
     * 
     * @param exp 表达式
     * @return 结果
     */
    private static String calculate(String exp) {
        exp = exp.replaceAll("[\\[\\]]", "");
        String number[] = exp.replaceFirst("(\\d)[\\+\\-\\*\\/]", "$1,").split(",");
        BigDecimal number1 = new BigDecimal(number[0]);
        BigDecimal number2 = new BigDecimal(number[1]);
        BigDecimal result = null;

        String operator = exp.replaceFirst("^.*\\d([\\+\\-\\*\\/]).+$", "$1");
        if ("+".equals(operator)) {
            result = number1.add(number2);
        } else if ("-".equals(operator)) {
            result = number1.subtract(number2);
        } else if ("*".equals(operator)) {
            result = number1.multiply(number2);
        } else if ("/".equals(operator)) {
            // 除数为0时直接返回0，防止报错
            if (number2.doubleValue() == 0)
                result = new BigDecimal(0);
            else
                // 第二个参数为精度，第三个为四色五入的模式
                result = number1.divide(number2, 4, RoundingMode.HALF_UP);
        }

        return result != null ? result.toString() : null;
    }

}