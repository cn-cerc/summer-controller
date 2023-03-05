package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.queue.TokenConfigImpl;
import cn.cerc.mis.core.LocalService;
import cn.cerc.mis.core.ServiceState;

public interface ServiceServerImpl {
    static final Logger log = LoggerFactory.getLogger(ServiceServerImpl.class);

    String getRequestUrl(IHandle handle, String service);

    void setConfig(TokenConfigImpl token);

    TokenConfigImpl getConfig(IHandle handle);

    default boolean isLocal(IHandle handle, ServiceSign service) {
        String url = this.getRequestUrl(handle, service.id());
        return url == null;
    }

    default DataSet call(ServiceSign service, IHandle handle, DataSet dataIn) {
        if (isLocal(handle, service))
            return LocalService.call(service.id(), handle, dataIn);

        String url = this.getRequestUrl(handle, service.id());
        try {
            Curl curl = new Curl();
            var config = this.getConfig(handle);
            if (config != null)
                this.getConfig(handle).getToken().ifPresent(token -> curl.put(ISession.TOKEN, token));
            curl.put("dataIn", dataIn.json());
            log.debug("request url: {}", url);
            log.debug("request params: {}", curl.getParameters());
            String response = curl.doPost(url);
            log.debug("response: {}", response);
            return new DataSet().setJson(response);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage(url + " remote service error");
        }
    }
}
