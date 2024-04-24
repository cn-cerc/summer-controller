package cn.cerc.mis.log;

public class JayunLogData {
    public static final String info = "info";
    public static final String warn = "warn";
    public static final String error = "error";
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
    private String line;
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
    private String[] stack;
    /**
     * 参数
     */
    private String args;
    /**
     * 创建时间
     */
    private long timestamp;

    /**
     * 负责人
     */
    private String mainName;

    /**
     * 修改人
     */
    private String name;

    /**
     * 修改时间
     */
    private String date;

    private JayunLogData(Builder builder) {
        this.hostname = builder.hostname;
        this.ip = builder.ip;
        this.port = builder.port;
        this.project = builder.project;
        this.token = builder.token;
        this.id = builder.id;
        this.line = builder.line;
        this.level = builder.level;
        this.message = builder.message;
        this.stack = builder.stack;
        this.args = builder.args;
        this.timestamp = builder.timestamp;
        this.mainName = builder.mainName;
        this.name = builder.name;
        this.date = builder.date;
    }

    public static class Builder {
        private String hostname;
        private String ip;
        private String port;
        private String project;
        private String token;
        private String id;
        private String line;
        private String level;
        private String message;
        private String[] stack;
        private String args;
        private long timestamp;
        private String mainName;
        private String name;
        private String date;

        public Builder(String id, String level, String message) {
            this.id = id;
            this.level = level;
            this.message = message;
            this.line = "?";
            this.timestamp(System.currentTimeMillis());
            this.hostname(ApplicationEnvironment.hostname());
            this.ip(ApplicationEnvironment.hostIP());
            this.port(ApplicationEnvironment.hostPort());
            this.project(JayunLogParser.loggerName());
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder line(String line) {
            this.line = line;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder stack(String[] stack) {
            this.stack = stack;
            return this;
        }

        public Builder args(String args) {
            this.args = args;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder mainName(String mainName) {
            this.mainName = mainName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public JayunLogData build() {
            return new JayunLogData(this);
        }
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

    public String getLine() {
        return this.line;
    }

    public void setLine(String line) {
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

    public String[] getStack() {
        return this.stack;
    }

    public void setStack(String[] stack) {
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

    public String getMainName() {
        return mainName;
    }

    public void setMainName(String mainName) {
        this.mainName = mainName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
