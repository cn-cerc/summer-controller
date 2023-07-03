package cn.cerc.mis.client;

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

    ISession getSession();

    /**
     * 
     * @return 判断当前帐套是否在本地
     */
    boolean isLocal();
}
