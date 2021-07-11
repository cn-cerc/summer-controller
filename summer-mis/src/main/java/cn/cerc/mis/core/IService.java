package cn.cerc.mis.core;

import cn.cerc.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface IService {

    DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException;

    boolean allowGuestUser(IHandle handle);

    // 仅用于 Delphi Client 调用
    @Deprecated
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.getJSON());
    }

}
