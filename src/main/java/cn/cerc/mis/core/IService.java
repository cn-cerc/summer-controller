package cn.cerc.mis.core;

import cn.cerc.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface IService extends IHandle {

    DataSet execute(DataSet dataIn) throws ServiceException;

    boolean checkSecurity(IHandle handle);
    
    String getRestPath();

    void setRestPath(String restPath);

    // 仅用于 Delphi Client 调用
    @Deprecated
    String getJSON(DataSet dataOut);

    @Deprecated
    default IStatus execute(DataSet dataIn, DataSet dataOut) throws ServiceException {
        dataOut = this.execute(dataIn);
        return new ServiceStatus(dataOut.getState(), dataOut.getMessage());
    }

}
