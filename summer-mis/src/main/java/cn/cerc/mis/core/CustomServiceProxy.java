package cn.cerc.mis.core;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Variant;

public abstract class CustomServiceProxy extends Handle {
    private String service;
    private DataSet dataIn;
    private DataSet dataOut;

    public CustomServiceProxy(IHandle handle) {
        super(handle);
    }

    protected Object getServiceObject(Variant function) {
        if (getSession() == null) {
            dataOut().setMessage("session is null.");
            return null;
        }
        if (service == null) {
            dataOut().setMessage("service is null.");
            return null;
        }

        try {
            return Application.getService(this, service(), function);
        } catch (ClassNotFoundException e) {
            dataOut().setMessage(e.getMessage());
            return null;
        }
    }

    public String service() {
        return service;
    }

    public CustomServiceProxy setService(String service) {
        this.service = service;
        return this;
    }

    public String message() {
        if (dataOut != null && dataOut.message() != null) {
            return dataOut.message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

    public DataSet dataIn() {
        if (dataIn == null)
            dataIn = new DataSet();
        return dataIn;
    }

    public void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

    public DataSet dataOut() {
        if (dataOut == null)
            dataOut = new DataSet();
        return dataOut;
    }

    protected void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

    @Deprecated
    public String getService() {
        return service();
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

}
