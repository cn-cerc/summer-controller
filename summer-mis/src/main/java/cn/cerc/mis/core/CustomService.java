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
import cn.cerc.mis.security.SecurityPolice;
import cn.cerc.mis.security.SecurityStopException;

//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
            this.dataIn = owner.dataIn();
            this.dataOut = owner.dataOut();
        }
        return this;
    }

    @Override
    public DataSet _call(IHandle handle, DataSet dataIn, KeyValue function) throws ServiceException {
        if (function == null || Utils.isEmpty(function.asString()))
            return new DataSet().setMessage("function is null");
        if ("_list".equals(function.asString()))
            return this._list();
        if ("_call".equals(function.asString()))
            return new DataSet().setMessage("function is call");
        if (Utils.isEmpty(this.funcCode))
            this.setFuncCode(function.asString());
        return this.execute(handle, dataIn);
    }

    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        this.setSession(handle.getSession());
        this.dataIn = dataIn;
        String funcCode = dataIn.head().getString("_function_");
        if (Utils.isEmpty(funcCode))
            funcCode = this.funcCode;
        else
            this.funcCode = funcCode;

        DataSet dataOut = new DataSet();
        this.dataOut = dataOut;

        if (Utils.isEmpty(funcCode))
            return dataOut.setMessage("function is null");

        Class<?> self = this.getClass();
        Method method;
        try {
            method = self.getMethod(funcCode);
        } catch (NoSuchMethodException | SecurityException e1) {
            method = null;
        }
        if (method == null) {
            try {
                method = self.getMethod(funcCode, IHandle.class, DataSet.class);
                if ("execute".equals(funcCode))
                    return dataOut.setMessage("function is execute");
            } catch (NoSuchMethodException | SecurityException e1) {
                method = null;
            }
        }
        if (method == null) {
            try {
                method = self.getMethod(funcCode, DataSet.class, DataSet.class);
            } catch (NoSuchMethodException | SecurityException e1) {
                method = null;
            }
        }

        if (method == null) {
            dataOut.setMessage(String.format("not find service: %s.%s ！", this.getClass().getName(), funcCode));
            dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            return dataOut;
        }

        try {
            if (!SecurityPolice.check(this, method, this)) {
                dataOut.setMessage(SecurityStopException.getAccessDisabled());
                dataOut.setState(ServiceState.ACCESS_DISABLED);
                return dataOut;
            }
            // 执行具体的服务函数
            if (method.getParameterCount() == 0) {
                int state = (Boolean) method.invoke(this) ? ServiceState.OK : ServiceState.ERROR;
                dataOut.setState(state);
            } else if (method.getParameterCount() == 1) {
                dataOut = (DataSet) method.invoke(this, dataIn);
                this.dataOut = dataOut;
            } else {
                if (method.getReturnType().equals(IStatus.class)) {
                    IStatus result = (IStatus) method.invoke(this, dataIn, dataOut);
                    if (dataOut.state() == ServiceState.ERROR)
                        dataOut.setState(result.getState());
                    if (dataOut.message() == null)
                        dataOut.setMessage(result.getMessage());
                } else {
                    dataOut = (DataSet) method.invoke(this, handle, dataIn);
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
                return dataOut.setState(ServiceState.ERROR).setMessage(msg);
            } else {
                log.error(msg, err);
                return dataOut.setState(ServiceState.ERROR).setMessage(msg);
            }
        }
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
        if (message == null || "".equals(message.trim()))
            return;
        dataOut().setMessage(message);
    }

    public String getFuncCode() {
        return this.funcCode;
    }

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

//    @Deprecated
//    public  void setProperty(String key, Object value) {
//        getSession().setProperty(key, value);
//    }

}
