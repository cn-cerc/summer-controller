package cn.cerc.mis.core;

public class ServiceState {
    // 执行成功
    public static final int OK = 1;
    // 以下为普通错误
    public static final int ERROR = 0;
    // 以下特定错误
    public static final int NOT_FIND_SERVICE = -1;
    // 无权限访问此服务器
    public static final int ACCESS_DISABLED = -2;
    // 调用超时
    public static final int CALL_TIMEOUT = -3;
}
