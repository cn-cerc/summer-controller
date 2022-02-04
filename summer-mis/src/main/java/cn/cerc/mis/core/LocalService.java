package cn.cerc.mis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.client.IServiceProxy;
import cn.cerc.mis.client.ServiceMeta;
import cn.cerc.mis.other.MemoryBuffer;

public class LocalService extends CustomServiceProxy implements IServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(LocalService.class);

    public LocalService(IHandle handle) {
        super(handle);
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
    }

    public LocalService(IHandle handle, ServiceMeta service) {
        this(handle);
        this.setService(service);
    }

    // 带缓存调用服务
    @Override
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
            if (object instanceof IHandle)
                ((IHandle) object).setSession(this.getSession());

            // 没有缓存时，直接读取并存入缓存
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

    @Override
    @Deprecated
    public LocalService setService(String service) {
        super.setService(service);
        return this;
    }
    
    @Override
    public LocalService setService(ServiceMeta service) {
        super.setService(service.id());
        return this;
    }

}
