package cn.cerc.mis.log;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.core.LastModified;
import cn.cerc.mis.security.SecurityStopException;

/**
 * 异常解析器用于读取堆栈的异常对象信息
 */
public class JayunLogParser {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static volatile JayunLogParser instance;
    private final String loggerName;

    /**
     * 通缉名单
     */
    private static final Set<String> wanted = new HashSet<>();
    static {
        wanted.add("site.diteng");
        wanted.add("site.obm");
        wanted.add("site.oem");
        wanted.add("site.odm");
        wanted.add("site.fpl");
    }
    /**
     * 采样分级
     */
    private static final List<Level> levels = new ArrayList<>();
    static {
        levels.add(Level.ERROR);
        levels.add(Level.WARN);
        levels.add(Level.INFO);
    }

    private JayunLogParser() {
        String loggerName = "";
        PropertyConfigurator.configure(JayunLogParser.class.getClassLoader().getResource("log4j.properties"));
        Logger logger = Logger.getRootLogger();
        Enumeration<?> allAppends = logger.getAllAppenders();
        Iterator<?> asIterator = allAppends.asIterator();
        while (asIterator.hasNext()) {
            if (asIterator.next() instanceof JayunLogAppender jayun) {
                loggerName = jayun.getName();
                break;
            }
        }
        this.loggerName = loggerName;
    }

    /**
     * 采集 log 默认的数据，不解析堆栈的业务对象
     */
    public static void analyze(String appender, final LoggingEvent event, final LocationInfo locationInfo) {
        executor.submit(() -> {
                        // 本地开发不发送日志到测试平台
            if (ServerConfig.isServerDevelop())
                return;
            // 灰度发布不发送日志到测试平台
            if (ServerConfig.isServerGray())
                return;
            // 将日志事件交给日志解析器处理
            Level level = event.getLevel();
            if (levels.stream().noneMatch(item -> level == item))
                return;

            // 获取类名和行号
            String className = locationInfo.getClassName();
            String lineNumber = locationInfo.getLineNumber();
            String message = event.getRenderedMessage();

            JayunLogData.Builder builder = new JayunLogData.Builder(className, level.toString().toLowerCase(), message)
                    .line(lineNumber)
                    .project(appender);

            // 检查日志事件是否包含异常
            if (event.getThrowableInformation() != null) {
                // 获取异常信息
                Throwable throwable = event.getThrowableInformation().getThrowable();
                if (throwable != null) {
                    if (throwable instanceof SecurityStopException) // 权限不足类警告写入 warn
                        builder.level(Level.WARN.toString().toLowerCase());
                }
            }

            // 读取起源类修改人
            try {
                String trigger = event.getLoggerName();
                Class<?> clazz = Class.forName(trigger);
                LastModified modified = clazz.getAnnotation(LastModified.class);
                if (modified != null) {
                    builder.mainName(modified.main());
                    builder.name(modified.name());
                    builder.date(modified.date());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            String[] stacks = event.getThrowableStrRep();
            if (stacks != null)
                builder.stack(stacks);

            // 起源类没有在通缉名单上再抓堆栈信息
            if (wanted.stream().noneMatch(className::contains)) {
                for (String stack : stacks) {
                    // 如果捕捉到业务代码就重置触发器信息
                    if (wanted.stream().anyMatch(stack::contains)) {
                        stack = stack.trim();
                        String trigger = JayunLogParser.trigger(stack);
                        if (Utils.isEmpty(trigger))
                            continue;
                        builder.id(trigger);

                        String line = JayunLogParser.lineNumber(stack);
                        builder.line(line);
                        try {
                            Class<?> caller = Class.forName(trigger);
                            // 读取通缉令修改人
                            LastModified modified = caller.getAnnotation(LastModified.class);
                            if (modified != null) {
                                builder.mainName(modified.main());
                                builder.name(modified.name());
                                builder.date(modified.date());
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
            new QueueJayunLog().push(builder.build());
        });
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

    public String getLoggerName() {
        return loggerName;
    }

    private static String trigger(String line) {
        // 定义正则表达式模式来匹配类的包名
        Pattern pattern = Pattern.compile("at\\s+([\\w.]+)\\..+");
        Matcher matcher = pattern.matcher(line);
        // 查找匹配项
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // 如果没有匹配项，则返回null
    }

    private static String lineNumber(String line) {
        String lineNumber = "?";
        int end = line.lastIndexOf(')');
        int begin = line.lastIndexOf(':', end - 1);
        if (begin == -1)
            return lineNumber;
        return line.substring(begin + 1, end);
    }

    public static void close() {
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        String line = "at site.diteng.start.login.WebDefault.execute(WebDefault.java:121)";
        System.out.println(trigger(line));
        System.out.println(lineNumber(line));
    }

}
