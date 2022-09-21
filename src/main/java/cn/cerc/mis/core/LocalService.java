package cn.cerc.mis.core;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.client.ServiceExport;
import cn.cerc.mis.client.ServiceProxy;
import cn.cerc.mis.client.ServiceSign;

public class LocalService extends ServiceProxy {
    private String service;

    public LocalService(IHandle handle) {
        super();
        this.setSession(handle.getSession());
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
    }

    @Deprecated
    public LocalService(IHandle handle, ServiceSign service) {
        this(handle);
        this.setService(service.id());
    }

    public LocalService setService(String service) {
        this.service = service;
        return this;
    }

    public boolean exec() {
        DataSet dataOut = LocalService.call(this.service, this, dataIn());
        this.setDataOut(dataOut);
        return this.isOk();
    }

    public boolean call(DataRow headIn) {
        dataIn().head().copyValues(headIn);
        DataSet dataOut = LocalService.call(this.service, this, dataIn());
        this.setDataOut(dataOut);
        return this.isOk();
    }

    public final String service() {
        return service;
    }

    @Override
    public String message() {
        if (super.dataOut() != null && super.dataOut().message() != null) {
            return super.dataOut().message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

    @Deprecated
    public void setBufferRead(boolean value) {
        // 此属性已被移除
    }

    @Deprecated
    public String getService() {
        return service;
    }

    @Deprecated
    public DataSet getDataIn() {
        return dataIn();
    }

    @Deprecated
    public DataSet getDataOut() {
        return dataOut();
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

    public static DataSet call(String service, IHandle handle, DataSet dataIn) {
        try {
            Variant function = new Variant("execute").setKey(service);
            IService bean = Application.getService(handle, service, function);
            return bean._call(handle, dataIn, function);
        } catch (ClassNotFoundException e) {
            return new DataSet().setMessage("not find service: " + service);
        } catch (ServiceException e) {
            return new DataSet().setMessage(e.getMessage());
        }
    }

    public String getExportKey() {
        return ServiceExport.build(this, this.dataIn());
    }

    public void setSign(ServiceSign sign) {
        this.service = sign.id();
    }

    @Deprecated
    public void setService(ServiceSign sign) {
        this.setSign(sign);
    }

    @Deprecated
    public String serviceId() {
        return this.service();
    }
}
