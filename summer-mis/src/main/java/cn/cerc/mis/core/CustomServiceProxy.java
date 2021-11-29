package cn.cerc.mis.core;

import cn.cerc.core.DataSet;
import cn.cerc.core.KeyValue;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;

public abstract class CustomServiceProxy extends Handle {
    private String service;
    private DataSet dataIn;
    private DataSet dataOut;

    public CustomServiceProxy(IHandle handle) {
        super(handle);
    }

    protected final Object getServiceObject(KeyValue function) {
        if (getSession() == null) {
            getDataOut().setMessage("session is null.");
            return null;
        }
        if (getService() == null) {
            getDataOut().setMessage("service is null.");
            return null;
        }

        try {
            return Application.getService(this, getService(), function);
        } catch (ClassNotFoundException e) {
            getDataOut().setMessage(e.getMessage());
            return null;
        }
    }

    public final String getService() {
        return service;
    }

    public final CustomServiceProxy setService(String service) {
        this.service = service;
        return this;
    }

    public final String getMessage() {
        if (dataOut != null && dataOut.message() != null) {
            return dataOut.message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

    public final DataSet getDataIn() {
        if (dataIn == null)
            dataIn = new DataSet();
        return dataIn;
    }

    public void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

    protected void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

    public final DataSet getDataOut() {
        if (dataOut == null)
            dataOut = new DataSet();
        return dataOut;
    }

}
