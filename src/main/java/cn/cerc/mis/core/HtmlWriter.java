package cn.cerc.mis.core;

import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;

public final class HtmlWriter {
    private final StringBuilder builder = new StringBuilder();

    public HtmlWriter print(String value) {
        builder.append(value);
        return this;
    }

    public HtmlWriter print(String format, Object... args) {
        builder.append(String.format(format, args));
        return this;
    }

    public HtmlWriter println(String value) {
        builder.append(value);
        if (ServerConfig.isServerDevelop()) {
            builder.append(Utils.separtor);
        }
        return this;
    }

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
