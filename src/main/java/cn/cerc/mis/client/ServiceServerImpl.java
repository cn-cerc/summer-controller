package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.mis.core.BookHandle;
import cn.cerc.mis.core.LocalService;
import cn.cerc.mis.core.ServiceState;

public interface ServiceServerImpl {

    Logger log = LoggerFactory.getLogger(ServiceServerImpl.class);

    String getRequestUrl(IHandle handle, String service);

    default String getOriginal() {
        return null;
    }

    TokenConfigImpl getDefaultConfig(IHandle handle);

    default boolean isLocal(IHandle handle, ServiceSign service) {
        if (getOriginal() != null) {
            if (ServerConfig.getAppOriginal().equals(getOriginal().toLowerCase())) {
                return true;
            }
        }
        String url = this.getRequestUrl(handle, service.id());
        return url == null;
    }

    default DataSet call(ServiceSign service, IHandle handle, DataSet dataIn) {
        if (isLocal(handle, service))
            return LocalService.call(service.id(), handle, dataIn);
        
        String url = this.getRequestUrl(handle, service.id());
        Curl curl = new Curl();
        TokenConfigImpl config = handle instanceof TokenConfigImpl ? (TokenConfigImpl) handle
                : this.getDefaultConfig(handle);
        config.getToken().ifPresent(token -> curl.put(ISession.TOKEN, token));
        curl.put("dataIn", dataIn.json());
        try {
            String response = curl.doPost(url);
            log.debug("response: {}", response);
            return new DataSet().setJson(response);
        } catch (IOException e) {
            log.error("{} , {} dataIn {} -> {}", url, curl.getParameters(), dataIn.json(), e.getMessage(), e);
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage(url + " remote service error");
        }
    }
}
