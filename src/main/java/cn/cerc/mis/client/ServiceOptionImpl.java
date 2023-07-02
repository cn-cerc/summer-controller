package cn.cerc.mis.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.IHandle;

/**
 * 用来满足特定的主机与token
 * 
 * @author 张弓
 *
 */
public interface ServiceOptionImpl {
    Logger log = LoggerFactory.getLogger(ServiceOptionImpl.class);

    /**
     * 
     * @return 指定帐套代码
     */
    default Optional<String> getCorpNo() {
        return Optional.empty();
    }

    /**
     * 
     * @param handle
     * @param service
     * @return 指定访问网址
     */
    default Optional<String> getEndpoint(IHandle handle, String service) {
        return Optional.empty();
    }

    /**
     * 
     * @return 指定访问 token
     */
    default Optional<String> getToken() {
        return Optional.empty();
    }

}
