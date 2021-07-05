package cn.cerc.mis.core;

import cn.cerc.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface IService extends IHandle {

    DataSet execute(DataSet dataIn) throws ServiceException;

    boolean allowGuestUser(IHandle handle);

    // 仅用于 Delphi Client 调用
    @Deprecated
    String getJSON(DataSet dataOut);

}
