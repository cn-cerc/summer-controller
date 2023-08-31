package cn.cerc.mis.math;

public interface IFunction {

    /**
     * 函数名称
     * 
     * @return name
     */
    String name();

    /**
     * 函数使用用法
     * 
     * @return description
     */
    String description();

    /**
     * 函数处理器
     * 
     * @param manage 函数管理器
     * @param text   表达式
     * @return 处理结果
     */
    String process(FunctionManager manage, String text);

    /**
     * 函数调用方法
     * 
     * @param manage   函数管理器
     * @param funcName 函数名
     * @param input    表达式
     * @return 结果
     */
    default String call(FunctionManager manage, String funcName, String input) {
        String prepare = manage.childProcess(input);
        String result = this.process(manage, prepare);
        // System.out.println(String.format("%s: input=%s, prepare=%s, result=%s",
        // funcName, input, prepare, result));
        return result;
    }

}
