package cn.cerc.mis.core;

import cn.cerc.core.ClassConfig;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.client.IServiceServer;

public class CenterServer implements IServiceServer {
    private static final ClassConfig config = new ClassConfig(CenterServer.class, SummerMIS.ID);
    private String site;

    public CenterServer() {
        site = config.getClassProperty("site", null);
    }
    
    @Override
    public String getToken(IHandle handle) {
        return null;
    }

    @Override
    public String getRequestUrl(String service) {
        return site == null ? null : String.format("%s?service=%s", site, service);
    }

}