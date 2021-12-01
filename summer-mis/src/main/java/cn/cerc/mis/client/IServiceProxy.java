package cn.cerc.mis.client;

import cn.cerc.core.DataSet;

public interface IServiceProxy {
    public static final String _message_ = "_message_";

    // 返回服务代码
    String getService();

    // 设置服务代码
    Object setService(String service);

    // 传入数据
    DataSet dataIn();

    @Deprecated
    default DataSet getDataIn() {
        return dataIn();
    }

    // 返回数据
    DataSet dataOut();

    @Deprecated
    default DataSet getDataOut() {
        return dataOut();
    }

    // 提示讯息
    String message();

    @Deprecated
    default String getMessage() {
        return message();
    }

    // 执行
    boolean exec(Object... args);

}
