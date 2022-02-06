package cn.cerc.mis.client;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.mis.core.BookHandle;
import cn.cerc.mis.core.ServiceQuery;

public class AutoService extends ServiceQuery {

    public AutoService(IHandle handle, String corpNo, String userCode, String service) {
        super(handle);
        super.setService(new ServiceSign(service));
        this.setSession(new BookHandle(this, corpNo).setUserCode(userCode).getSession());
    }

    public boolean exec() throws ServiceException {
        return super.call(dataIn()).isOk();
    }

    public String message() {
        if (super.dataOut() != null && super.dataOut().message() != null) {
            return super.dataOut().message().replaceAll("'", "\"");
        } else {
            return null;
        }
    }

}
