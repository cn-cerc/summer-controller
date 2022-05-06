package cn.cerc.mis.core;

import java.util.Map;
import java.util.Objects;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.mis.client.ServiceExecuteException;
import cn.cerc.mis.client.ServiceSign;
import cn.cerc.mis.other.MemoryBuffer;

public class ServiceQuery implements IHandle {
    private ServiceSign service;
    private DataSet dataIn;
    private DataSet dataOut;
    private ISession session;

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataSet dataIn) {
        return new ServiceQuery(handle, service).call(dataIn);
    }

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, service).call(dataIn);
    }

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
        this.dataIn = dataIn;
        this.dataOut = this.service.call(this, dataIn);
        return this;
    }

    public ServiceQuery setService(ServiceSign service) {
        this.service = service;
        return this;
    }

    public boolean isOk() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() > 0;
    }

    public boolean isOkElseThrow() throws ServiceExecuteException {
        if (!isOk())
            throw new ServiceExecuteException(dataOut.message());
        return true;
    }

    public boolean isFail() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() <= 0;
    }

    public DataSet getDataOutElseThrow() throws ServiceExecuteException {
        Objects.requireNonNull(dataOut);
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut;
    }

    public DataRow getHeadOutElseThrow() throws ServiceExecuteException {
        Objects.requireNonNull(dataOut);
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut.head();
    }

    public final DataSet dataIn() {
        if (this.dataIn == null)
            this.dataIn = new DataSet();
        return dataIn;
    }

    public final DataSet dataOut() {
        if (this.dataOut == null)
            this.dataOut = new DataSet();
        return dataOut;
    }

    public String getExportKey() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, this.getUserCode(), timestamp)) {
            buff.setValue("data", this.dataIn().json());
        }
        return timestamp;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    public final String serviceId() {
        return service.id();
    }

}
