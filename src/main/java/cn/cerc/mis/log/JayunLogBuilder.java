package cn.cerc.mis.log;

public class JayunLogBuilder {
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
     * 类名
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
     * 异常类型
     */
    private String exception;
    /**
     * 堆栈信息
     */
    private String[] stack;
    /**
     * 异常对象的参数
     */
    private Object args;
    /**
     * 创建时间
     */
    private long createTime;
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

    public JayunLogBuilder(String id, String level, String message) {
        this.id = id;
        this.level = level;
        this.message = message;
        init();
    }

    private void init() {
        this.line = "?";
        this.createTime = System.currentTimeMillis();
        this.hostname = ApplicationEnvironment.hostname();
        this.ip = ApplicationEnvironment.hostIP();
        this.port = ApplicationEnvironment.hostPort();
        this.project = JayunLogParser.loggerName();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainName() {
        return mainName;
    }

    public void setMainName(String mainName) {
        this.mainName = mainName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Object getArgs() {
        return args;
    }

    public void setArgs(Object args) {
        this.args = args;
    }

    public String[] getStack() {
        return stack;
    }

    public void setStack(String[] stack) {
        this.stack = stack;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
