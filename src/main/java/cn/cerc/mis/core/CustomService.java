package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;

public abstract class CustomService extends Handle implements IService {

    @Autowired
    public ISystemTable systemTable;

    // 单例模式下不能使用下述变量
    private DataSet dataIn = null; // request
    private DataSet dataOut = null; // response
    private String funcCode;

    public CustomService init(CustomService owner, boolean refData) {
        this.setSession(owner.getSession());
        if (refData) {
            this.dataIn = owner.dataIn();
            this.dataOut = owner.dataOut();
        }
        return this;
    }

    @Override
    public DataSet _call(IHandle handle, DataSet dataIn, Variant function) throws IllegalAccessException,
            InvocationTargetException, ServiceException, DataException, RuntimeException {
        if (function == null || Utils.isEmpty(function.getString()))
            return new DataSet().setMessage("function is null");
        if ("_list".equals(function.getString()))
            return this._list();
        if ("_call".equals(function.getString()))
            return new DataSet().setMessage("function is call");
        if (Utils.isEmpty(this.funcCode))
            this.funcCode = function.getString();

        this.setSession(handle.getSession());
        this.dataIn = dataIn;
        String funcCode = dataIn.head().getString("_function_");
        if (Utils.isEmpty(funcCode))
            funcCode = this.funcCode;
        else
            this.funcCode = funcCode;

        this.dataOut = new DataSet();
        if (Utils.isEmpty(funcCode))
            return dataOut.setMessage("function is null");

        Class<?> self = this.getClass();
        ServiceMethod sm = ServiceMethod.build(self, funcCode);
        if (sm == null) {
            dataOut.setMessage(String.format("not find service: %s.%s ！", this.getClass().getName(), funcCode));
            dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            return dataOut;
        }
        this.dataOut = sm.call(this, handle, dataIn);
        return dataOut;
    }

    public DataSet dataIn() {
        if (this.dataIn == null)
            this.dataIn = new DataSet();
        return this.dataIn;
    }

    @Deprecated
    public DataSet getDataIn() {
        return dataIn();
    }

    public DataSet dataOut() {
        if (this.dataOut == null)
            this.dataOut = new DataSet();
        return this.dataOut;
    }

    @Deprecated
    public DataSet getDataOut() {
        return dataOut();
    }

    public boolean fail(String message) {
        dataOut().setMessage(message);
        return false;
    }

    public String message() {
        return dataOut().message();
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty())
            return;
        dataOut().setMessage(message);
    }

    public String getFuncCode() {
        return this.funcCode;
    }

    // FishingO2O项目还有使用XML配置，移除后才能删除
    public void setFuncCode(String funcCode) {
        this.funcCode = funcCode;
    }

    public IStatus success() {
        return new ServiceStatus(ServiceState.OK);
    }

    public IStatus success(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.OK);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    public IStatus fail(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.ERROR);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    @Deprecated
    public Object getProperty(String key) {
        return getSession().getProperty(key);
    }

}
