package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.IHandle;

public interface ServerConfigImpl {
    /**
     * 
     * @return 本地主机，ip + port
     */
    String localhost();

    /**
     * 相同的主机之间，使用 localhost 调用，否则使用 website 调用
     * 
     * @return 主机组代码
     */
    String group();

    /**
     * 
     * @return 远程主机，网址 + port
     */
    String website();

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
