package cn.cerc.mis.core;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.mis.client.ServiceExecuteException;
import cn.cerc.mis.client.ServiceSign;

public class ServiceQuery implements IHandle {
    protected ServiceSign service;
    protected DataSet dataIn;
    protected DataSet dataOut;
    private ISession session;

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataSet dataIn) {
        ServiceQuery svr = new ServiceQuery(handle, service);
        svr.call(dataIn);
        return svr;
    }

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        ServiceQuery svr = new ServiceQuery(handle, service);
        svr.call(dataIn);
        return svr;
    }

    public static ServiceQuery open(IHandle handle, ServiceSign service, Map<String, Object> headIn) {
        Objects.requireNonNull(headIn);
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        ServiceQuery svr = new ServiceQuery(handle, service);
        svr.call(dataIn);
        return svr;
    }

    public ServiceQuery call(DataSet dataIn) {
        this.dataIn = dataIn;
        dataOut = this.service.call(this, dataIn);
        return this;
    }

    public ServiceQuery(IHandle handle) {
        this.setSession(handle.getSession());
    }

    public ServiceQuery(IHandle handle, ServiceSign service) {
        this(handle);
        this.service = service;
    }

    public ServiceQuery setService(ServiceSign service) {
        this.service = service;
        return this;
    }

    public boolean isOk() {
        return dataOut.state() > 0;
    }

    public boolean isFail() {
        return dataOut.state() <= 0;
    }

    public DataSet get() {
        return dataOut;
    }

    public DataSet getElseThrow() throws ServiceExecuteException {
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut;
    }

    public <X extends Throwable> DataSet getElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (dataOut.state() <= 0)
            throw exceptionSupplier.get();
        return dataOut;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

}
