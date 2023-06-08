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

    default String getApplication() {
        return null;
    };

    TokenConfigImpl getDefaultConfig(IHandle handle);

    default boolean isLocal(IHandle handle, ServiceSign service) {
        if (getApplication() != null) {
            if (ServerConfig.getAppOriginal().equals(getApplication().toLowerCase())) {
                return true;
            }
        }
        String url = this.getRequestUrl(handle, service.id());
        return url == null;
    }

    // http调用
    default DataSet call(ServiceSign service, IHandle handle, DataSet dataIn) {
        TokenConfigImpl config = handle instanceof TokenConfigImpl ? (TokenConfigImpl) handle
                : this.getDefaultConfig(handle);
        // 本地调用
        if (isLocal(handle, service)) {
            var bookHandle = handle;
            if (config != null && config.getBookNo().isPresent()) {
                var bookNo = config.getBookNo().get();
                if (!bookNo.equals(handle.getCorpNo()))
                    bookHandle = new BookHandle(handle, bookNo);
            }
            return LocalService.call(service.id(), bookHandle, dataIn);
        }
        // 远程调用
        String url = this.getRequestUrl(handle, service.id());
        Curl curl = new Curl();
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
