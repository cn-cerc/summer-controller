package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.mis.core.ServiceState;

public interface IServiceServer {
    static final Logger log = LoggerFactory.getLogger(IServiceServer.class);

    String getRequestUrl(IHandle handle, String service);

    String getToken(IHandle handle);

    default DataSet _call(IHandle handle, DataSet dataIn, String serviceId) {
        try {
            Curl curl = new Curl();
            String token = this.getToken(handle);
            if (token != null)
                curl.put(ISession.TOKEN, token);
            curl.put("dataIn", dataIn.json());
            String url = this.getRequestUrl(handle, serviceId);
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
