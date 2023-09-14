package cn.cerc.mis.core;

import cn.cerc.db.core.DataException;

public class DataValidateException extends DataException {

    private static final long serialVersionUID = -5965584243989194951L;

    public DataValidateException(String errorMsg) {
        super(errorMsg);
    }

    // 满足条件即抛出错误
    public static void stopRun(String errorMsg, boolean stopValue) throws DataValidateException {
        if (stopValue) {
            throw new DataValidateException(errorMsg);
        }
    }

    public static void stopRun(String errorMsg, String dataValue, String stopValue) throws DataValidateException {
        if (stopValue.equals(dataValue)) {
            throw new DataValidateException(errorMsg);
        }
    }

    public static void stopRun(String errorMsg, int dataValue, int stopValue) throws DataValidateException {
        if (stopValue == dataValue) {
            throw new DataValidateException(errorMsg);
        }

    }
}
