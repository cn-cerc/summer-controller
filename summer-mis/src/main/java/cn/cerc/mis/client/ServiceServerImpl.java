package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.mis.core.ServiceState;

public interface ServiceServerImpl {
    static final Logger log = LoggerFactory.getLogger(ServiceServerImpl.class);

    String getRequestUrl(IHandle handle, String service);

    String getToken(IHandle handle);

    default DataSet call(ServiceSign service, IHandle handle, DataSet dataIn) {
        String url = this.getRequestUrl(handle, service.id());
        if (url == null)
            return LocalServer.call(service, handle, dataIn);

        try {
            Curl curl = new Curl();
            String token = this.getToken(handle);
            if (token != null)
                curl.put(ISession.TOKEN, token);
            curl.put("dataIn", dataIn.json());
            log.debug("request url: {}", url);
            log.debug("request params: {}", curl.getParameters());
            String response = curl.doPost(url);
            log.debug("response: {}", response);
            return new DataSet().setJson(response);
        } catch (IOException e) {
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage("remote service error");
        }
    }
}
