package cn.cerc.mis.client;

import cn.cerc.db.core.ServiceException;

/**
 * 服务执行异常
 */
public class ServiceExecuteException extends ServiceException {
    private static final long serialVersionUID = 665054396844872223L;

    public ServiceExecuteException(String message) {
        super(message);
    }
}
