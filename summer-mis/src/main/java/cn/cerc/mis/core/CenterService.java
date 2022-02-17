package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;
import cn.cerc.mis.client.ServiceServerImpl;
import cn.cerc.mis.client.RemoteService;
import cn.cerc.mis.client.ServiceSign;

/**
 * 调用中心数据库权限等服务
 * 
 * @author ZhangGong
 *
 */
public class CenterService extends RemoteService {
    private static final ServiceServerImpl server = new CenterServer();

    public CenterService(IHandle handle) {
        super(handle);
    }

    public CenterService setService(String serviceId) {
        super.setService(new ServiceSign(serviceId, server));
        return this;
    }

}
