package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.IHandle;

public interface ServerConfigImpl {

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
