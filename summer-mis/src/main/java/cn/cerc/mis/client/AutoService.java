package cn.cerc.mis.client;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.BookHandle;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceQuery;

public class AutoService extends ServiceQuery {
    private String corpNo;
    private String userCode;

    public AutoService(IHandle handle, String corpNo, String userCode, String service) {
        super(handle);
        this.corpNo = corpNo;
        this.userCode = userCode;
        this.setService(service);
    }

    public boolean exec() throws ServiceException {
        if (this.service() == null) {
            throw new RuntimeException("not specified service");
        }

        Variant function = new Variant("execute").setTag(service());
        Object object = getServiceObject(function);
        if (object == null) {
            return false;
        }

        BookHandle handle = new BookHandle(this, getCorpNo());
        handle.setUserCode(getUserCode());
        if (object instanceof IHandle) {
            ((IHandle) object).setSession(handle.getSession());
        }

        setDataOut(((IService) object)._call(handle, dataIn(), function));
        return dataOut().state() > 0;
    }

    public String service() {
        return service.id();
    }

    @Override
    public String getCorpNo() {
        return corpNo;
    }

    public void setCorpNo(String corpNo) {
        this.corpNo = corpNo;
    }

    @Override
    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public AutoService setService(String service) {
        super.setService(new ServiceSign(service));
        return this;
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
            return Application.getService(this, service.id(), function);
        } catch (ClassNotFoundException e) {
            dataOut().setMessage(e.getMessage());
            return null;
        }
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
