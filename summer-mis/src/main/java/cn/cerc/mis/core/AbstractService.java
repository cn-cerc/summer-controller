package cn.cerc.mis.core;

import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.core.DataSet;
import cn.cerc.core.ISession;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;

//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractService extends Handle implements IService {
    protected DataSet dataOut = new DataSet();
    @Autowired
    public ISystemTable systemTable;

    @Override
    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        this.setSession(handle.getSession());
        IStatus status = execute(dataIn, dataOut);
        if (dataOut.getState() == ServiceState.ERROR)
            dataOut.setState(status.getState());
        if (dataOut.getMessage() == null)
            dataOut.setMessage(status.getMessage());
        // 防止调用者修改并回写到数据库
        dataOut.disableStorage();
        return dataOut;
    }

    public final IStatus success() {
        return new ServiceStatus(ServiceState.OK);
    }

    public final IStatus success(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.OK);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    public final IStatus fail(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.ERROR);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    // 主要适用于Delphi Client调用
    @Override
    public final boolean allowGuestUser(IHandle handle) {
        ISession sess = handle.getSession();
        return sess != null && sess.logon();
    }

    public abstract IStatus execute(DataSet dataIn, DataSet dataOut) throws ServiceException;

}
