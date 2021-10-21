package cn.cerc.mis.security;

import cn.cerc.core.ISession;
import cn.cerc.mis.core.IService;

public interface SecurityService extends IService {

    public boolean initSession(ISession session, String token);

    public String getPermissions(ISession session);

}
