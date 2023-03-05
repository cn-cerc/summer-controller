package cn.cerc.mis.core;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.queue.TokenConfig;
import cn.cerc.db.queue.TokenConfigImpl;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.client.ServiceServerImpl;

public class CenterServer implements ServiceServerImpl {
    private static final ClassConfig config = new ClassConfig(CenterServer.class, SummerMIS.ID);
    private String site;

    public CenterServer() {
        site = config.getClassProperty("site", null);
    }

    @Override
    public String getRequestUrl(IHandle handle, String service) {
        return site == null ? null : String.format("%s?service=%s", site, service);
    }

    @Override
    public TokenConfigImpl getConfig(IHandle handle) {
        return new TokenConfig();
    }

    @Override
    public void setConfig(TokenConfigImpl config) {
        throw new RuntimeException("此服务不支持配置 token");
    }

}