package cn.cerc.mis.log;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import cn.cerc.db.core.Utils;
import cn.cerc.db.zk.ZkNode;
import cn.cerc.db.zk.ZkServer;

public class JayunLog implements Appender {
    private String name;
    private Layout layout;
    private ErrorHandler errorHandler;
    private Map<String, String> items = new HashMap<>();

    public void initMap(String name) {
        if (ZkServer.get() == null)
            return;
        String[] levels = new String[] { "info", "warn", "error" };
        for (String level : levels) {
            String token = ZkNode.get()
                    .getNodeValue(String.format("%s/%s/log/%s", QueueJayunLog.prefix, name, level), () -> "");
            items.put(level, token);
        }
    }

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
            if (items.isEmpty())
                initMap(name);
            var data = new JayunLogData(event);
            String token = items.get(data.getLevel());
            if (Utils.isEmpty(token))
                return;
            data.setToken(token);
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
