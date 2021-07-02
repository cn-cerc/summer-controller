package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.core.ClassResource;
import cn.cerc.core.DataSet;
import cn.cerc.core.ISession;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.SummerMIS;

public abstract class CustomService extends Handle implements IService {
    private static final Logger log = LoggerFactory.getLogger(CustomService.class);
    private static final ClassResource res = new ClassResource(CustomService.class, SummerMIS.ID);
    @Autowired
    public ISystemTable systemTable;
    protected DataSet dataIn = null; // request
    protected DataSet dataOut = null; // response
    protected String funcCode;

    public CustomService init(CustomService owner, boolean refData) {
        this.setSession(owner.getSession());
        if (refData) {
            this.dataIn = owner.getDataIn();
            this.dataOut = owner.getDataOut();
        }
        return this;
    }

    @Override
    public DataSet execute(DataSet dataIn) throws ServiceException {
        this.setDataIn(dataIn);

        if (this.funcCode == null)
            throw new RuntimeException("funcCode is null");

        Class<?> self = this.getClass();
        Method mt = null;
        for (Method item : self.getMethods()) {
            if (item.getName().equals(this.funcCode)) {
                mt = item;
                break;
            }
        }
        if (mt == null) {
            this.setMessage(
                    String.format(res.getString(1, "没有找到服务：%s.%s ！"), this.getClass().getName(), this.funcCode));
            dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            return dataOut;
        }

        try {
            long startTime = System.currentTimeMillis();
            try {
                // 执行具体的服务函数
                if (mt.getParameterCount() == 0) {
                    getDataOut().setState((Boolean) mt.invoke(this) ? ServiceState.OK : ServiceState.ERROR);
                    getDataOut().setMessage(this.getMessage());
                    return dataOut;
                } else if (mt.getParameterCount() == 1) {
                    dataOut = (DataSet) mt.invoke(this, dataIn);
                    return dataOut;
                } else {
                    IStatus result = (IStatus) mt.invoke(this, dataIn, dataOut);
                    if (dataOut.getState() == ServiceState.ERROR)
                        dataOut.setState(result.getState());
                    if (dataOut.getMessage() == null)
                        dataOut.setMessage(result.getMessage());
                    return dataOut;
                }
            } finally {
                if (dataOut != null) {
                    dataOut.first();
                }
                long totalTime = System.currentTimeMillis() - startTime;
                long timeout = 1000;
                if (totalTime > timeout) {
                    String[] tmp = this.getClass().getName().split("\\.");
                    String service = tmp[tmp.length - 1] + "." + this.funcCode;
                    log.warn(String.format("corpNo:%s, userCode:%s, service:%s, tickCount:%s", getCorpNo(),
                            getUserCode(), service, totalTime));
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Throwable err = e.getCause() != null ? e.getCause() : e;
            String msg = err.getMessage() == null ? "error is null" : err.getMessage();
            if ((err instanceof ServiceException)) {
                setMessage(msg);
                getDataOut().setState(ServiceState.ERROR);
                return dataOut;
            } else {
                log.error(msg, err);
                setMessage(msg);
                getDataOut().setState(ServiceState.ERROR);
            }
            return getDataOut();
        }
    }

    public final DataSet getDataIn() {
        if (dataIn == null) {
            dataIn = new DataSet();
        }
        return dataIn;
    }

    public final DataSet getDataOut() {
        if (dataOut == null) {
            dataOut = new DataSet();
        }
        return dataOut;
    }

    public final boolean fail(String message) {
        getDataOut().setMessage(message);
        return false;
    }

    public final String getMessage() {
        return getDataOut().getMessage();
    }

    public final void setMessage(String message) {
        if (message == null || "".equals(message.trim()))
            return;
        getDataOut().setMessage(message);
    }

    // 设置是否需要授权才能登入
    @Override
    public boolean checkSecurity(IHandle handle) {
        ISession sess = handle.getSession();
        return sess != null && sess.logon();
    }

    @Override
    public String getJSON(DataSet dataOut) {
        return String.format("[%s]", this.getDataOut().getJSON());
    }

    public final String getFuncCode() {
        return funcCode;
    }

    public final void setFuncCode(String funcCode) {
        this.funcCode = funcCode;
    }

    public final void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

    public final void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

    public final IStatus success() {
        return new ServiceStatus(ServiceState.OK);
    }

    public final IStatus success(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.OK);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    public final IStatus fail(String format, Object... args) {
        ServiceStatus status = new ServiceStatus(ServiceState.ERROR);
        if (args.length > 0) {
            status.setMessage(String.format(format, args));
        } else {
            status.setMessage(format);
        }
        return status;
    }

    @Deprecated
    public final Object getProperty(String key) {
        return getSession().getProperty(key);
    }

//    @Deprecated
//    public final void setProperty(String key, Object value) {
//        getSession().setProperty(key, value);
//    }

}
