package cn.cerc.mis.client;

import cn.cerc.db.core.ServiceException;

public class ServiceQueryException extends ServiceException {
    private static final long serialVersionUID = 665054396844872223L;

    public ServiceQueryException(String message) {
        super(message);
    }
}
