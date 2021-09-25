package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import cn.cerc.core.ClassResource;
import cn.cerc.core.DataSet;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

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
    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        this.setSession(handle.getSession());
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
                } else if (mt.getParameterCount() == 1) {
                    dataOut = (DataSet) mt.invoke(this, dataIn);
                } else {
                    IStatus result = (IStatus) mt.invoke(this, dataIn, getDataOut());
                    if (dataOut.getState() == ServiceState.ERROR)
                        dataOut.setState(result.getState());
                    if (dataOut.getMessage() == null)
                        dataOut.setMessage(result.getMessage());
                }
                // 防止调用者修改并回写到数据库
                dataOut.disableStorage();
                return dataOut;
            } finally {
                if (dataOut != null) {
                    dataOut.first();
                }
                long totalTime = System.currentTimeMillis() - startTime;
                if (totalTime > 1000) {
                    String[] tmp = this.getClass().getName().split("\\.");
                    String service = tmp[tmp.length - 1] + "." + this.funcCode;
                    TimeOut timeOut = new TimeOut();
                    timeOut.setProject(ServerConfig.getAppName());
                    timeOut.setCorpNo(getCorpNo());
                    timeOut.setUserCode(getUserCode());
                    timeOut.setService(service);
                    timeOut.setTimer(totalTime);
                    timeOut.setDataIn(dataIn.toJson());
                    String json = new Gson().toJson(timeOut);
                    log.warn(json);
                    try (Jedis redis = JedisFactory.getJedis()) {
                        String key = MemoryBuffer.buildKey(SystemBuffer.Global.TimeOut);
                        redis.lpush(key, json);
                    }
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

    public class TimeOut {
        private String project;
        private String corpNo;
        private String userCode;
        private String service;
        private long timer;
        private String dataIn;

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getCorpNo() {
            return corpNo;
        }

        public void setCorpNo(String corpNo) {
            this.corpNo = corpNo;
        }

        public String getUserCode() {
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public long getTimer() {
            return timer;
        }

        public void setTimer(long timer) {
            this.timer = timer;
        }

        public String getDataIn() {
            return dataIn;
        }

        public void setDataIn(String dataIn) {
            this.dataIn = dataIn;
        }

    }

}
