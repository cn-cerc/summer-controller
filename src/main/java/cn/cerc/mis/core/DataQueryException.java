package cn.cerc.mis.core;

import cn.cerc.db.core.DataException;

/**
 * 数据查询异常
 */
public class DataQueryException extends DataException {

    private static final long serialVersionUID = 8952694781483616836L;

    public DataQueryException(String message) {
        super(message);
    }

    public DataQueryException(String format, Object... args) {
        super(String.format(format, args));
    }

}