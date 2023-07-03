package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.IHandle;

public interface ServerConfigImpl {
    /**
     * 
     * @return 内网地址，ip + port
     */
    String intranet();

    /**
     * 相同的主机之间，使用 intranet 调用，否则使用 extranet 调用
     * 
     * @return 主机分组代码
     */
    String group();

    /**
     * 
     * @return 外网入口，网址 + port
     */
    String extranet();

    /**
     * 
     * @param handle
     * @param corpNo 帐套代码
     * @return 返回对方授权 token
     */
    Optional<String> getEndpoint(IHandle handle, String corpNo);

    /**
     * 
     * @param handle
     * @param corpNo 被调用的帐套代码
     * @return 返回可授权的 token
     */
    Optional<String> getToken(IHandle handle, String corpNo);
}
