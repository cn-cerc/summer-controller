package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.ISession;

/**
 * 跨集群主机token支持
 * 
 * @author 张弓
 *
 */
public interface CorpConfigImpl {

    /**
     * 
     * @return 企业原始帐套代码
     */
    String getCorpNo();

    /**
     * 
     * @return 返回代理服务器
     */
    default Optional<ServiceOptionImpl> getServer() {
        return Optional.empty();
    }

    ISession getSession();
}
