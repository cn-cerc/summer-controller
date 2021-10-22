package cn.cerc.mis.core;

import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;

//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractService extends Handle implements IService {
    @Autowired
    public ISystemTable systemTable;

    public final DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        this.setSession(handle.getSession());
        DataSet dataOut = new DataSet();
        IStatus status = execute(dataIn, dataOut);
        if (dataOut.getState() == ServiceState.ERROR)
            dataOut.setState(status.getState());
        if (dataOut.getMessage() == null)
            dataOut.setMessage(status.getMessage());
        // 防止调用者修改并回写到数据库
        dataOut.disableStorage();
        return dataOut;
    }

    public abstract IStatus execute(DataSet dataIn, DataSet dataOut) throws ServiceException;

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

}
