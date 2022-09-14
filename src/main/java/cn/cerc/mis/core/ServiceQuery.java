package cn.cerc.mis.core;

import java.util.Map;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.client.ServiceExport;
import cn.cerc.mis.client.ServiceProxy;
import cn.cerc.mis.client.ServiceSign;

public class ServiceQuery extends ServiceProxy {
    private ServiceSign service;

    @Deprecated
    public static ServiceQuery open(IHandle handle, ServiceSign service, DataSet dataIn) {
        return new ServiceQuery(handle, service).call(dataIn);
    }

    @Deprecated
    public static ServiceQuery open(IHandle handle, ServiceSign service, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, service).call(dataIn);
    }

    /**
     * 外部使用请改为 DataRow.of() 代替 Map.of
     */
    @Deprecated
    public static ServiceQuery open(IHandle handle, ServiceSign service, Map<String, Object> headIn) {
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return new ServiceQuery(handle, service).call(dataIn);
    }

    public ServiceQuery(IHandle handle) {
        this.setSession(handle.getSession());
    }

    public ServiceQuery(IHandle handle, ServiceSign service) {
        this(handle);
        this.service = service;
    }

    public ServiceQuery call(DataSet dataIn) {
        this.setDataIn(dataIn);
        this.setDataOut(this.service.call(this, dataIn).dataOut());
        return this;
    }

    public ServiceQuery setService(ServiceSign service) {
        this.service = service;
        return this;
    }

    public String getExportKey() {
        return ServiceExport.build(this, this.dataIn());
    }

    public final String serviceId() {
        return service.id();
    }

}
