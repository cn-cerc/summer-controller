package cn.cerc.mis.client;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface ServiceSignImpl {

    ServiceSign sign();

    ServiceSign callLocal(IHandle handle, DataSet dataIn);

    ServiceSign callRemote(CorpConfigImpl config, DataSet dataIn);

}