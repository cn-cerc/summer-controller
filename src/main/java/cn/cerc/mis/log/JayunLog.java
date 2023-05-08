package cn.cerc.mis.log;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import cn.cerc.db.core.ServerConfig;

public class JayunLog implements Appender {
    private String name;
    private Layout layout;
    private ErrorHandler errorHandler;

    @Override
    public void addFilter(Filter newFilter) {

    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void clearFilters() {

    }

    @Override
    public void close() {

    }

    @Override
    public void doAppend(LoggingEvent event) {
        if (event.getLevel() == Level.ERROR || event.getLevel() == Level.WARN) {
            // 本地开发不发送日志到测试平台
            if (ServerConfig.isServerDevelop())
                return;
            // 灰度发布不发送日志到测试平台
            if (ServerConfig.isServerGray())
                return;
            var data = new JayunLogData(event);
            data.setProject(this.name);
            new QueueJayunLog().push(data);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

}
