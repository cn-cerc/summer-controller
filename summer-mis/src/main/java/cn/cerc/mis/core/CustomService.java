package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.core.DataSet;
import cn.cerc.core.KeyValue;
import cn.cerc.core.Utils;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;

public abstract class CustomService extends Handle implements IService {
    private static final Logger log = LoggerFactory.getLogger(CustomService.class);

    @Autowired
    public ISystemTable systemTable;

    // 单例模式下不能使用下述变量
    private DataSet dataIn = null; // request
    private DataSet dataOut = null; // response
    private String funcCode;

    public CustomService init(CustomService owner, boolean refData) {
        this.setSession(owner.getSession());
        if (refData) {
            this.dataIn = owner.getDataIn();
            this.dataOut = owner.getDataOut();
        }
        return this;
    }

    @Override
    public DataSet call(IHandle handle, DataSet dataIn, KeyValue function) throws ServiceException {
        if (function == null || Utils.isEmpty(function.asString()))
            return new DataSet().setMessage("function is null");
        if ("call".equals(function.asString()))
            return new DataSet().setMessage("function is call");
        if (Utils.isEmpty(this.funcCode))
            this.setFuncCode(function.asString());
        return this.execute(handle, dataIn);
    }

    
    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        this.setSession(handle.getSession());
        this.dataIn = dataIn;
        String funcCode = dataIn.getHead().getString("_function_");
        if (Utils.isEmpty(funcCode))
            funcCode = this.funcCode;
        else
            this.funcCode = funcCode;

        DataSet dataOut = new DataSet();
        this.dataOut = dataOut;

        if (Utils.isEmpty(funcCode))
            return dataOut.setMessage("haed[_function_] is null");

        Class<?> self = this.getClass();
        Method mt = null;
        for (Method item : self.getMethods()) {
            if (item.getName().equals(funcCode)) {
                mt = item;
                break;
            }
        }

        if (mt == null) {
            dataOut.setMessage(String.format("not find service: %s.%s ！", this.getClass().getName(), funcCode));
            dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            return dataOut;
        }

        try {
            // 执行具体的服务函数
            if (mt.getParameterCount() == 0) {
                int state = (Boolean) mt.invoke(this) ? ServiceState.OK : ServiceState.ERROR;
                dataOut.setState(state);
            } else if (mt.getParameterCount() == 1) {
                dataOut = (DataSet) mt.invoke(this, dataIn);
                this.dataOut = dataOut;
            } else {
                if (mt.getReturnType().equals(IStatus.class)) {
                    IStatus result = (IStatus) mt.invoke(this, dataIn, dataOut);
                    if (dataOut.getState() == ServiceState.ERROR)
                        dataOut.setState(result.getState());
                    if (dataOut.getMessage() == null)
                        dataOut.setMessage(result.getMessage());
                } else {
                    dataOut = (DataSet) mt.invoke(this, handle, dataIn);
                    this.dataOut = dataOut;
                }
            }
            // 防止调用者修改并回写到数据库
            dataOut.disableStorage();
            dataOut.first();
            return dataOut;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Throwable err = e.getCause() != null ? e.getCause() : e;
            String msg = err.getMessage() == null ? "error is null" : err.getMessage();
            if ((err instanceof ServiceException)) {
                return new DataSet().setState(ServiceState.ERROR).setMessage(msg);
            } else {
                log.error(msg, err);
                return new DataSet().setState(ServiceState.ERROR).setMessage(msg);
            }
        }
    }

    public  DataSet getDataIn() {
        if (this.dataIn == null)
            this.dataIn = new DataSet();
        return this.dataIn;
    }

    public  DataSet getDataOut() {
        if (this.dataOut == null)
            this.dataOut = new DataSet();
        return this.dataOut;
    }

    public  boolean fail(String message) {
        getDataOut().setMessage(message);
        return false;
    }

    public  String getMessage() {
        return getDataOut().getMessage();
    }

    public  void setMessage(String message) {
        if (message == null || "".equals(message.trim()))
            return;
        getDataOut().setMessage(message);
    }

    public  String getFuncCode() {
        return this.funcCode;
    }

    public  void setFuncCode(String funcCode) {
        this.funcCode = funcCode;
    }

    public  IStatus success() {
        return new ServiceStatus(ServiceState.OK);
    }

    public  IStatus success(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.OK);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    public  IStatus fail(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.ERROR);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    @Deprecated
    public  Object getProperty(String key) {
        return getSession().getProperty(key);
    }

//    @Deprecated
//    public  void setProperty(String key, Object value) {
//        getSession().setProperty(key, value);
//    }

}
