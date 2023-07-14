package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.IHandle;

public interface ServerConfigImpl {

    /**
     * 获取指定帐套的产业别
     * 
     * @param handle 句柄
     * @param corpNo 被调用的帐套代码
     * @return
     */
    Optional<String> getIndustry(IHandle handle, String corpNo);

    /**
     * 获取指定帐套的服务节点
     * 
     * @param handle 句柄
     * @param corpNo 被调用的帐套代码
     * @return 返回对方授权 token
     */
    Optional<String> getEndpoint(IHandle handle, String corpNo);

    /**
     * 获取指定帐套的授权代码
     * 
     * @param handle 句柄
     * @param corpNo 被调用的帐套代码
     * @return 返回可授权的 token
     */
    Optional<String> getToken(IHandle handle, String corpNo);
}
