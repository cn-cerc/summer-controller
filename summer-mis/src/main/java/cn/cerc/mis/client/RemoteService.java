package cn.cerc.mis.client;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.core.ServiceQuery;

public class RemoteService extends ServiceQuery {
    private static final ClassResource res = new ClassResource(RemoteService.class, SummerMIS.ID);

    public RemoteService(IHandle handle) {
        super(handle);
    }

    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = dataIn().head();
            if (args.length % 2 != 0)
                throw new RuntimeException(res.getString(1, "传入的参数数量必须为偶数！"));
            for (int i = 0; i < args.length; i = i + 2)
                headIn.setValue(args[i].toString(), args[i + 1]);
        }

        return super.call(dataIn()).isOk();
    }

    public final String getService() {
        return service.id();
    }

    public final String message() {
        return dataOut().message();
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

}
