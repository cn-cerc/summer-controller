package cn.cerc.mis.exceptions;

import cn.cerc.mis.exception.IKnowall;

public class ServiceMethodExecuteException extends RuntimeException implements IKnowall {

    private String[] data = new String[0];

    public ServiceMethodExecuteException(Throwable cause) {
        super(cause);
    }

    // 运行异常, clientIP {}, token {}, service {}, corpNo {}, dataIn {}, message {}
    public ServiceMethodExecuteException(String clientIP, String token, String corpNo, String dataIn, Throwable cause) {
        super(cause);
        this.data = new String[] { "clientIP " + clientIP, "token " + token, "corpNo " + corpNo, "dataIn " + dataIn };
    }

    @Override
    public String[] getData() {
        return data;
    }

}
