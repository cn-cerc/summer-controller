package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;
import cn.cerc.mis.client.IServiceServer;
import cn.cerc.mis.client.RemoteService;

/**
 * 调用中心数据库权限等服务
 * 
 * @author ZhangGong
 *
 */
public class CenterService extends RemoteService {
    private static final IServiceServer server = new CenterServer();

    public CenterService(IHandle handle) {
        super(handle);
        this.setServer(server);
    }

}
