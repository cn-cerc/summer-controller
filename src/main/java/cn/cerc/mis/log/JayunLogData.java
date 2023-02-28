package cn.cerc.mis.log;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class JayunLogData {
    public static final String Info = "info";
    public static final String Warn = "warn";
    public static final String Error = "error";
    /**
     * 项目
     */
    private String project;
    /**
     * 授权码
     */
    private String token;
    /**
     * 类名+行号
     */
    private String id;
    /**
     * 行号
     */
    private int line;
    /**
     * 日志等级 (info\warn\error)
     */
    private String level;
    /**
     * 报错信息
     */
    private String message;
    /**
     * 堆栈信息
     */
    private List<String> stack;
    /**
     * 参数
     */
    private String args;
    /**
     * 创建时间
     */
    private long timestamp;

    public JayunLogData() {
    }

    public JayunLogData(LoggingEvent event) {
        LocationInfo locationInfo = event.getLocationInformation();
        this.id = locationInfo.getClassName();
        this.line = Integer.parseInt(locationInfo.getLineNumber());
        if (event.getLevel() == Level.ERROR)
            this.level = Error;
        else if (event.getLevel() == Level.WARN)
            this.level = Warn;
        else
            this.level = Info;
        this.message = event.getRenderedMessage();
        ThrowableInformation throwableInfo = event.getThrowableInformation();
        if (throwableInfo == null)
            this.stack = null;
        else
            this.stack = Arrays.asList(throwableInfo.getThrowableStrRep());
        this.timestamp = event.getTimeStamp();
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getStack() {
        return stack;
    }

    public void setStack(List<String> stack) {
        this.stack = stack;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
