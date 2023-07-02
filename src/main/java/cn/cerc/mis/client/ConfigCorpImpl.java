package cn.cerc.mis.client;

import cn.cerc.db.core.ISession;

/**
 * 跨集群主机token支持
 * 
 * @author 张弓
 *
 */
public interface ConfigCorpImpl {

    /**
     * 
     * @return 企业原始帐套代码
     */
    String getCorpNo();

    ISession getSession();
}
