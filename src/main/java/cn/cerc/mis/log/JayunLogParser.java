package cn.cerc.mis.log;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.DefaultThrowableRenderer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.client.ServiceSign;
import cn.cerc.mis.core.LastModified;
import cn.cerc.mis.register.center.ApplicationEnvironment;
import cn.cerc.mis.security.SecurityStopException;

/**
 * 异常解析器用于读取堆栈的异常对象信息
 */
public class JayunLogParser {
    private static volatile JayunLogParser instance;
    private final String loggerName;

    private JayunLogParser() {
        String loggerName = "";
        PropertyConfigurator.configure(ServiceSign.class.getClassLoader().getResource("log4j.properties"));
        Logger logger = Logger.getRootLogger();
        Enumeration<?> allAppends = logger.getAllAppenders();
        Iterator<?> asIterator = allAppends.asIterator();
        while (asIterator.hasNext()) {
            if (asIterator.next() instanceof JayunLog jayun) {
                loggerName = jayun.getName();
                break;
            }
        }
        this.loggerName = loggerName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    /**
     * 用于反向获取 log4j.properties 配置的 JayunLog 别名
     */
    public static String loggerName() {
        if (instance == null) {
            synchronized (JayunLogParser.class) {
                if (instance == null)
                    instance = new JayunLogParser();
            }
        }
        return instance.getLoggerName();
    }

    /**
     * 警告类日志
     */
    public static void warn(String className, LastModified modified, Throwable throwable, String message) {
        JayunLogParser.analyze(className, modified, throwable, message, JayunLogData.warn);
    }

    /**
     * 错误类日志
     */
    public static void error(String className, LastModified modified, Throwable throwable, String message) {
        JayunLogParser.analyze(className, modified, throwable, message, JayunLogData.error);
    }

    private static void analyze(String className, LastModified modified, Throwable throwable, String message,
            String level) {
        if (throwable == null)
            return;

        // 数据集异常不采集
        if (throwable instanceof DataException)
            return;

        if (Utils.isEmpty(JayunLogParser.loggerName()))
            return;

        JayunLogData data = new JayunLogData();
        data.setId(className);
        data.setLine("?");

        // 权限不足设为警告
        if (throwable instanceof SecurityStopException)
            data.setLevel(JayunLogData.warn);
        else
            data.setLevel(level);
        data.setMessage(message);

        String[] stack = DefaultThrowableRenderer.render(throwable);
        for (String line : stack) {
            // 如果捕捉到业务代码就重置触发器信息
            if (line.contains("site.diteng")) {
                line = line.trim();
                String trigger = JayunLogParser.trigger(line);
                if (Utils.isEmpty(trigger))
                    continue;
                data.setId(trigger);
                data.setLine(JayunLogParser.lineNumber(line));

                try {
                    Class<?> clazz = Class.forName(trigger);
                    modified = clazz.getAnnotation(LastModified.class);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        if (modified != null) {
            data.setName(modified.name());
            data.setDate(modified.date());
        }

        data.setStack(stack);
        data.setTimestamp(System.currentTimeMillis());
        data.setHostname(ApplicationEnvironment.hostname());
        data.setIp(ApplicationEnvironment.hostIP());
        data.setPort(ApplicationEnvironment.hostPort());
        data.setProject(JayunLogParser.loggerName());
        // 推送数据到日志监控队列
        new QueueJayunLog().push(data);
    }

    public static String trigger(String line) {
        // 定义正则表达式模式来匹配类的包名
        Pattern pattern = Pattern.compile("at\\s+([\\w.]+)\\..+");
        Matcher matcher = pattern.matcher(line);
        // 查找匹配项
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // 如果没有匹配项，则返回null
    }

    public static String lineNumber(String line) {
        String lineNumber = "?";
        int iend = line.lastIndexOf(')');
        int ibegin = line.lastIndexOf(':', iend - 1);
        if (ibegin == -1)
            return lineNumber;
        return line.substring(ibegin + 1, iend);
    }

    public static void main(String[] args) {
        String line = "at site.diteng.start.login.WebDefault.execute(WebDefault.java:121)";
        System.out.println(trigger(line));
        System.out.println(lineNumber(line));
    }

}
