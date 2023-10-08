package cn.cerc.mis.core;

import cn.cerc.db.core.DataException;

/**
 * 权限不足检查异常
 */
public class PassportCheckException extends DataException {

    private static final long serialVersionUID = 8952694781483616836L;

    public PassportCheckException(String message) {
        super(message);
    }

    public PassportCheckException(String format, Object... args) {
        super(String.format(format, args));
    }

}