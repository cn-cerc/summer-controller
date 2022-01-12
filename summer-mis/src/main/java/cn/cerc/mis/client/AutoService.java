package cn.cerc.mis.client;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.BookHandle;
import cn.cerc.mis.core.CustomServiceProxy;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceException;

public class AutoService extends CustomServiceProxy {
    private String corpNo;
    private String userCode;

    public AutoService(IHandle handle, String corpNo, String userCode, String service) {
        super(handle);
        this.corpNo = corpNo;
        this.userCode = userCode;
        this.setService(service);
    }

    public boolean exec() throws ServiceException {
        if (this.service() == null) {
            throw new RuntimeException("not specified service");
        }

        Variant function = new Variant("execute").setTag(service());
        Object object = getServiceObject(function);
        if (object == null) {
            return false;
        }

        BookHandle handle = new BookHandle(this, getCorpNo());
        handle.setUserCode(getUserCode());
        if (object instanceof IHandle) {
            ((IHandle) object).setSession(handle.getSession());
        }

        setDataOut(((IService) object)._call(handle, dataIn(), function));
        return dataOut().state() > 0;
    }

    @Override
    public String getCorpNo() {
        return corpNo;
    }
    
    public void setCorpNo(String corpNo) {
        this.corpNo = corpNo;
    }

    @Override
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

}
