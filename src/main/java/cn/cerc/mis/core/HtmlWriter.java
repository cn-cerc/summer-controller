package cn.cerc.mis.core;

import org.apache.xmlbeans.impl.common.IOUtil;

import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;

public final class HtmlWriter extends IOUtil implements IWriter {
    private final StringBuilder builder = new StringBuilder();

    @Override
    public HtmlWriter print(Object value) {
        builder.append(value);
        return this;
    }

    @Override
    public HtmlWriter print(String format, Object... args) {
        builder.append(String.format(format, args));
        return this;
    }

    @Override
    public HtmlWriter println(Object value) {
        builder.append(value);
        if (ServerConfig.isServerDevelop()) {
            builder.append(Utils.separtor);
        }
        return this;
    }

    @Override
    public HtmlWriter println(String format, Object... args) {
        builder.append(String.format(format, args));
        if (ServerConfig.isServerDevelop()) {
            builder.append(Utils.separtor);
        }
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
