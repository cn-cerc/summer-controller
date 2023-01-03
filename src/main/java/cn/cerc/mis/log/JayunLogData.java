package cn.cerc.mis.log;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

public class JayunLogData {
    /**
     * 项目错误/警告日志表
     */
    public static final String T_Project_Log = "t_project_log";
    public static final int Info = 0;
    public static final int Warn = 1;
    public static final int Error = 2;

    private String project;
    private String source;
    private long timestamp;
    private String message;
    private int level = JayunLogData.Info;

    public JayunLogData() {

    }

    public JayunLogData(LoggingEvent event) {
        this.source = event.getLoggerName();
        this.timestamp = event.getTimeStamp();
        this.message = event.getRenderedMessage();
        if (event.getLevel() == Level.WARN)
            this.level = JayunLogData.Warn;
        event.getLevel();
        if (event.getLevel() == Level.ERROR)
            this.level = JayunLogData.Error;
        else
            this.level = JayunLogData.Info;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}