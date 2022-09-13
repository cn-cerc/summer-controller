package cn.cerc.mis.client;

import java.util.List;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface ServiceSignImpl {

    ServiceSign sign();

    ServiceSign call(IHandle handle);

    ServiceSign call(IHandle handle, DataRow headIn);

    ServiceSign call(IHandle handle, DataSet dataIn);

    Object head();

    List<?> body();

}