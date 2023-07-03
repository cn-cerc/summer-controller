package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.LocalService;
import cn.cerc.mis.core.ServiceState;

public class RemoteService extends ServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(RemoteService.class);
    private static final ClassResource res = new ClassResource(RemoteService.class, SummerMIS.ID);
    private ServiceSign sign;

    public RemoteService(IHandle handle) {
        super();
        this.setSession(handle.getSession());
    }

    public ServiceSign sign() {
        return this.sign;
    }

    public void setSign(ServiceSign sign) {
        this.sign = sign;
    }

    @Deprecated
    public void setService(ServiceSign sign) {
        this.setSign(sign);
    }

    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = dataIn().head();
            if (args.length % 2 != 0)
                throw new RuntimeException(res.getString(1, "传入的参数数量必须为偶数！"));
            for (int i = 0; i < args.length; i = i + 2)
                headIn.setValue(args[i].toString(), args[i + 1]);
        }
        this.setDataOut(this.sign.call(this, this.dataIn()).dataOut());
        return this.isOk();
    }

    @Deprecated
    public final String getService() {
        return sign.id();
    }

    @Deprecated
    public String getMessage() {
        return message();
    }

    /**
     * 调用远程服务
     * 
     * @param endpoint 远程服务器 url
     * @param token    远程服务器访问 token
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet call(String endpoint, String token, String service, DataSet dataIn) {
        Curl curl = new Curl();
        if (!Utils.isEmpty(token))
            curl.put(ISession.TOKEN, token);
        if (Utils.isEmpty(service))
            throw new RuntimeException("service is null");
        curl.put("dataIn", dataIn.json());
        try {
            String response = curl.doPost(endpoint + service);
            return new DataSet().setJson(response);
        } catch (IOException | JsonSyntaxException e) {
            log.error("{}{} , {} -> {}", endpoint, service, curl.getParameters(), e.getMessage(), e);
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage("remote service error");
        }
    }

    /**
     * 根据帐套配置，调用相应的机群服务
     * 
     * @param handle
     * @param targetCorpNo 根据被调用目标帐套，获取 endpoint 与 token 并调用
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet call(IHandle handle, String targetCorpNo, String service, DataSet dataIn) {
        String corpNo = handle.getCorpNo();
        if ("000000".equals(targetCorpNo) && ServerConfig.isCspOriginal()) {
            log.warn("别瞎搞，csp 自身应改使用 callLocal 来调用 {}", service);
            return LocalService.call(service, handle, dataIn);
        }

        if (targetCorpNo.equals(corpNo)) {
            log.warn("调用逻辑错误，发起帐套和目标帐套相同，应改使用 callLocal 来调用 {}", service);
            return LocalService.call(service, handle, dataIn);
        }

        var server = Application.getBean(ServerConfigImpl.class);
        String endpoint = server.getEndpoint(handle, targetCorpNo)
                .orElseThrow(() -> new RuntimeException("无法获取到有效的访问节点"));
        String token = server.getToken(handle, targetCorpNo).orElse(null);
        return call(endpoint, token, service, dataIn);
    }

    /**
     * 
     * @param handle
     * @param serviceOption 根据 service 配置，获取 endpoint 与 token 并调用
     * @param service
     * @param dataIn
     * @return
     */
    public static DataSet call(IHandle handle, ServerOptionImpl serviceOption, String service, DataSet dataIn) {
        String endpoint = serviceOption.getEndpoint(handle, service).orElseThrow();
        String token = serviceOption.getToken().orElseThrow();
        return call(endpoint, token, service, dataIn);
    }

}
