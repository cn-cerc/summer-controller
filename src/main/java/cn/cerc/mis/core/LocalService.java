package cn.cerc.mis.core;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.client.ServiceSign;

public class LocalService extends ServiceQuery {

    public LocalService(IHandle handle) {
        super(handle);
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        super.setService(new ServiceSign(service));
    }

    public LocalService(IHandle handle, ServiceSign service) {
        this(handle);
        this.setService(service);
    }

    public LocalService setService(String service) {
        super.setService(new ServiceSign(service));
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

        return super.call(dataIn()).isOk();
    }

    public final String service() {
        return serviceId();
    }

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
        return serviceId();
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
