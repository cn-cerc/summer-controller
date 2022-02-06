package cn.cerc.mis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.client.ServiceSign;
import cn.cerc.mis.other.MemoryBuffer;

public class LocalService extends ServiceQuery {
    private static final Logger log = LoggerFactory.getLogger(LocalService.class);

    public LocalService(IHandle handle) {
        super(handle);
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
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

        Variant function = new Variant("execute").setTag(service());
        Object object = getServiceObject(function);
        if (object == null)
            return false;

        try {
            setDataOut(((IService) object)._call(this, dataIn(), function));
            return dataOut().state() > 0;
        } catch (Exception e) {
            Throwable err = e;
            if (e.getCause() != null)
                err = e.getCause();
            log.error(err.getMessage(), err);
            dataOut().setState(ServiceState.ERROR).setMessage(err.getMessage());
            return false;
        }
    }

    public String getExportKey() {
        String tmp = "" + System.currentTimeMillis();
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, this.getUserCode(), tmp)) {
            buff.setValue("data", this.dataIn().json());
        }
        return tmp;
    }

    @Deprecated
    public void setBufferRead(boolean value) {
        // 此属性已被移除
    }

    public String service() {
        return service.id();
    }

    @Deprecated
    public String getService() {
        return service.id();
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

    public LocalService setService(String service) {
        super.setService(new ServiceSign(service));
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
