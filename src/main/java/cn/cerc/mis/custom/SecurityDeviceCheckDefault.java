package cn.cerc.mis.custom;

import org.springframework.stereotype.Component;

import cn.cerc.db.core.ISession;
import cn.cerc.mis.core.IForm;
import cn.cerc.mis.core.ISecurityDeviceCheck;
import cn.cerc.mis.core.SecurityDevice;

@Component
public class SecurityDeviceCheckDefault implements ISecurityDeviceCheck {

    private ISession session;

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    @Override
    public SecurityDevice pass(IForm form) {
        return SecurityDevice.permit;
    }

}
