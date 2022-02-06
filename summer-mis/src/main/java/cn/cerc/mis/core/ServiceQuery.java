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
    private DataSet dataIn;
    private DataSet dataOut;
    private ISession session;

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

    public ServiceQuery call(DataRow headIn) {
        this.dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return this.call(dataIn);
    }

    public ServiceQuery call(Map<String, Object> headIn) {
        this.dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return this.call(dataIn);
    }

    public ServiceQuery setService(ServiceSign service) {
        this.service = service;
        return this;
    }

    public boolean isOk() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() > 0;
    }

    public boolean isFail() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() <= 0;
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

    public DataSet getDataOutElseThrow() throws ServiceExecuteException {
        Objects.requireNonNull(dataOut);
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut;
    }

    public <X extends Throwable> DataSet getElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(dataOut);
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
