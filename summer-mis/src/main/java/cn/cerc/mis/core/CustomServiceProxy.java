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

    public String service() {
        return service;
    }

    @Deprecated
    public final String getService() {
        return service();
    }

    public final CustomServiceProxy setService(String service) {
        this.service = service;
        return this;
    }

    public final String message() {
        if (dataOut != null && dataOut.message() != null) {
            return dataOut.message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

    public DataSet dataIn() {
        if (dataIn == null)
            dataIn = new DataSet();
        return dataIn;
    }

    @Deprecated
    public final DataSet getDataIn() {
        return dataIn();
    }

    public void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

    public DataSet dataOut() {
        if (dataOut == null)
            dataOut = new DataSet();
        return dataOut;
    }

    @Deprecated
    public final DataSet getDataOut() {
        return dataOut();
    }

    public void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

}
