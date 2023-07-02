package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceState;

public class ServiceUtils {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);

    /**
     * 
     * @param handle
     * @param service 服务代码
     * @param dataIn  调用参数
     * @return
     */
    public static DataSet callLocal(IHandle handle, String service, DataSet dataIn) {
        try {
            Variant function = new Variant("execute").setKey(service);
            IService bean = Application.getService(handle, service, function);
            return bean._call(handle, dataIn, function);
        } catch (ClassNotFoundException e) {
            return new DataSet().setMessage("not find service: " + service);
        } catch (ServiceException e) {
            return new DataSet().setMessage(e.getMessage());
        }
    }

    /**
     * 调用远程服务
     * 
     * @param endpoint 远程服务器url
     * @param token    远程服务器访问凭据
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet callRemote(String endpoint, String token, String service, DataSet dataIn) {
        Curl curl = new Curl();
        if (!Utils.isEmpty(token))
            curl.put(ISession.TOKEN, token);
        curl.put("dataIn", dataIn.json());
        try {
            String response = curl.doPost(endpoint);
            return new DataSet().setJson(response);
        } catch (IOException | JsonSyntaxException e) {
            log.error("{} , {} -> {}", endpoint, curl.getParameters(), e.getMessage(), e);
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage("remote service error");
        }
    }

    /**
     * 根据帐套配置，调用相应的机群服务
     * 
     * @param handle
     * @param corpNo
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet callRemote(IHandle handle, String corpNo, String service, DataSet dataIn) {
        var curCorp = handle.getCorpNo();
        if (curCorp.equals(corpNo)) {
            log.warn("应改使用 callLocal 来调用 {}", service);
            return callLocal(handle, service, dataIn);
        }
        var config = Application.getBean(ServiceConfigImpl.class);
        String endpoint = config.getEndpoint(handle, corpNo).orElseThrow();
        String token = config.getToken(handle, corpNo).orElseThrow();
        return callRemote(endpoint, token, service, dataIn);
    }

    /**
     * 仅用于调用中心库
     * 
     * @param handle
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet callCenter(IHandle handle, String service, DataSet dataIn) {
        if ("csp".equals(ServerConfig.getAppOriginal())) {
            return ServiceUtils.callLocal(handle, service, dataIn);
        } else {
            var registerServer = Application.getBean(ServiceRegister.class);
            var endpoint = registerServer.getServiceSite("csp");
            return ServiceUtils.callRemote(endpoint.website(), handle.getSession().getToken(), service, dataIn);
        }
    }

}
