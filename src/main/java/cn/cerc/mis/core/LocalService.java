package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.db.log.KnowallLog;
import cn.cerc.mis.client.ServiceExport;
import cn.cerc.mis.client.ServiceProxy;
import cn.cerc.mis.client.ServiceSign;
import cn.cerc.mis.security.SecurityStopException;

/**
 * 提供本地服务访问
 */
public class LocalService extends ServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(LocalService.class);
    private String service;

    public LocalService(IHandle handle) {
        super();
        this.setSession(handle.getSession());
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
    }

    @Deprecated
    public LocalService(IHandle handle, ServiceSign service) {
        this(handle);
        this.setService(service.id());
    }

    public LocalService setService(String service) {
        this.service = service;
        return this;
    }

    // 带缓存调用服务
    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = dataIn().head();
            if (args.length % 2 != 0)
                throw new RuntimeException("传入的参数数量必须为偶数！");
            for (int i = 0; i < args.length; i = i + 2)
                headIn.setValue(args[i].toString(), args[i + 1]);
        }

        DataSet dataOut = LocalService.call(this.service, this, dataIn());
        this.setDataOut(dataOut);
        return this.isOk();
    }

    public final String service() {
        return service;
    }

    @Override
    public String message() {
        if (super.dataOut() != null && super.dataOut().message() != null) {
            return super.dataOut().message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

    @Deprecated
    public void setBufferRead(boolean value) {
        // 此属性已被移除
    }

    @Deprecated
    public String getService() {
        return service;
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

    public static DataSet call(String key, IHandle handle, DataSet dataIn) {
        DataSet dataOut = new DataSet();
        IService service;
        Variant function = new Variant("execute").setKey(key);
        try {
            service = Application.getService(handle, key, function);
        } catch (ClassNotFoundException e) {
            log.warn(e.getMessage(), e);
            return dataOut.setError().setMessage("not find service: " + key);
        }

        try {
            return service._call(handle, dataIn, function);
        } catch (Exception e) {
            Throwable throwable = e.getCause() != null ? e.getCause() : e;

            if (throwable instanceof IllegalArgumentException)
                log.error("参数异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);
            else if (throwable instanceof InvocationTargetException)
                log.error("反射异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);
            else if (throwable instanceof ServiceException)
                log.error("服务异常, service {}, corpNo {}, message {}", key, handle.getCorpNo(), throwable.getMessage(),
                        KnowallLog.of(throwable, dataIn.json()));
            else if (throwable instanceof DataException)
                log.warn("数据异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);
            else if (throwable instanceof SecurityStopException)
                log.warn("权限异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);
            else if (throwable instanceof RuntimeException)
                log.error("运行异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);
            else
                log.error("其他异常, service {}, corpNo {}, dataIn {}, message {}", key, handle.getCorpNo(), dataIn.json(),
                        throwable.getMessage(), throwable);

            dataOut.setError().setMessage(throwable.getMessage());
            return dataOut;
        }
    }

    public String getExportKey() {
        return ServiceExport.build(this, this.dataIn());
    }

    public void setSign(ServiceSign sign) {
        this.service = sign.id();
    }

    @Deprecated
    public void setService(ServiceSign sign) {
        this.setSign(sign);
    }

    @Deprecated
    public String serviceId() {
        return this.service();
    }

}
