package cn.cerc.mis.core;

import cn.cerc.core.DataSet;
import cn.cerc.core.ISession;
import cn.cerc.db.core.IHandle;

public interface IService extends IHandle {

    IStatus execute(DataSet dataIn, DataSet dataOut) throws ServiceException;

    default IStatus fail(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(0);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    default IStatus success() {
        return new ServiceStatus(1);
    }

    default IStatus success(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(1);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    // 主要适用于Delphi Client调用
    default boolean checkSecurity(IHandle handle) {
        ISession sess = handle.getSession();
        return sess != null && sess.logon();
    }

    // 主要适用于Delphi Client调用
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.getJSON());
    }

    @Deprecated
    default Object getProperty(String key) {
        return getSession().getProperty(key);
    }

    @Deprecated
    default void setProperty(String key, Object value) {
        getSession().setProperty(key, value);
    }

}
