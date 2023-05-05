package cn.cerc.mis.client;

import java.util.List;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface ServiceSignImpl {

    ServiceSign sign();

    @Deprecated
    default ServiceSign call(IHandle handle) {
        return callLocal(handle);
    }

    @Deprecated
    default ServiceSign call(IHandle handle, DataRow headIn) {
        return callLocal(handle, headIn);
    }

    @Deprecated
    default ServiceSign call(IHandle handle, DataSet dataIn) {
        return callLocal(handle, dataIn);
    }

    default ServiceSign callLocal(IHandle handle) {
        return callLocal(handle, new DataSet());
    }

    default ServiceSign callLocal(IHandle handle, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return callLocal(handle, dataIn);
    }

    ServiceSign callLocal(IHandle handle, DataSet dataIn);

    default ServiceSign callRemote(TokenConfigImpl config) {
        return callRemote(config, new DataSet());
    }

    default ServiceSign callRemote(TokenConfigImpl config, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return callRemote(config, dataIn);
    }

    ServiceSign callRemote(TokenConfigImpl config, DataSet dataIn);

    Object head();

    List<?> body();

}