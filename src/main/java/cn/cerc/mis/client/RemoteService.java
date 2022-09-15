package cn.cerc.mis.client;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.SummerMIS;

public class RemoteService extends ServiceProxy {
    private static final ClassResource res = new ClassResource(RemoteService.class, SummerMIS.ID);
    private ServiceSign sign;

    public RemoteService(IHandle handle) {
        super();
        this.setSession(handle.getSession());
    }

    public ServiceSign sign() {
        return this.sign;
    }

    public void setSign(ServiceSign sign) {
        this.sign = sign;
    }

    @Deprecated
    public void setService(ServiceSign sign) {
        this.setSign(sign);
    }

    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = dataIn().head();
            if (args.length % 2 != 0)
                throw new RuntimeException(res.getString(1, "传入的参数数量必须为偶数！"));
            for (int i = 0; i < args.length; i = i + 2)
                headIn.setValue(args[i].toString(), args[i + 1]);
        }

        this.setDataOut(this.sign.call(this, this.dataIn()).dataOut());
        return this.isOk();

    }

    @Deprecated
    public final String getService() {
        return sign.id();
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

}
