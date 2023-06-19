package cn.cerc.mis.log;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import cn.cerc.mis.register.center.ApplicationEnvironment;

public class JayunLogData {
    public static String info = "info";
    public static String warn = "warn";
    public static String error = "error";
    /**
     * 主机名
     */
    private String hostname;
    /**
     * ip地址
     */
    private String ip;
    /**
     * 端口
     */
    private String port;
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
            this.level = error;
        else if (event.getLevel() == Level.WARN)
            this.level = warn;
        else
            this.level = info;
        this.message = event.getRenderedMessage();
        ThrowableInformation throwableInfo = event.getThrowableInformation();
        if (throwableInfo == null)
            this.stack = null;
        else
            this.stack = Arrays.asList(throwableInfo.getThrowableStrRep());
        this.timestamp = event.getTimeStamp();
        this.hostname = ApplicationEnvironment.hostname();
        this.ip = ApplicationEnvironment.hostIP();
        Optional<String> portOpt = ApplicationEnvironment.hostPort();
        if (portOpt.isPresent())
            this.port = portOpt.get();
    }

    public String getProject() {
        return this.project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLine() {
        return this.line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getLevel() {
        return this.level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getStack() {
        return this.stack;
    }

    public void setStack(List<String> stack) {
        this.stack = stack;
    }

    public String getArgs() {
        return this.args;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

}
