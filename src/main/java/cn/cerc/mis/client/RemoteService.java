package cn.cerc.mis.client;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonSyntaxException;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.log.KnowallLog;
import cn.cerc.local.tool.JsonTool;
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
     * @param service  服务签名 SvrUserInfo.search
     * @param dataIn   服务参数
     * @return 服务返回值
     */
    public static DataSet call(String endpoint, String token, String service, DataSet dataIn) {
        Curl curl = new Curl();
        if (!Utils.isEmpty(token))
            curl.put(ISession.TOKEN, token);
        if (Utils.isEmpty(service))
            throw new RuntimeException("service is null");
        curl.put("dataIn", dataIn.json());
        String response = "";
        try {
            response = curl.doPost(endpoint + service);
            return new DataSet().setJson(response);
        } catch (IOException | JsonSyntaxException e) {
            log.error("{}{} 远程服务访问异常 {}", endpoint, service, e.getMessage(),
                    KnowallLog.of(e).add("入参信息", JsonTool.toJson(curl.getParameters())).add("返回信息", response));
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage("remote service error");
        }
    }

    /**
     * 根据帐套配置，调用相应的机群服务
     */
    public static DataSet call(IHandle handle, CorpConfigImpl targetConfig, String key, DataSet dataIn,
            ServerOptionImpl serviceOption) throws ServiceException {
        Objects.requireNonNull(targetConfig);
        // 防止本地调用
        if (targetConfig.isLocal()) {
            if (!"000000".equals(targetConfig.getCorpNo())) {
                String message = String.format("%s, %s 发起帐套和目标帐套相同，应改使用 callLocal 来调用，dataIn %s", key,
                        handle.getCorpNo(), dataIn.json());
                RuntimeException e = new RuntimeException(message);
                log.warn("{}", message, e);
            }
            return LocalService.call(key, handle, dataIn);
        } else if (serviceOption != null) {
            // 处理特殊的业务场景，创建帐套、钓友商城
            // 获取指定的目标机节点
            String endpoint = serviceOption.getEndpoint(handle, key).orElse(null);
            // 获取指定的目标机授权
            var token = serviceOption.getToken(handle).orElse(null);
            if (endpoint == null || token == null) {
                // 用于自建的私服企业
                var server = RemoteService.getServerConfig(Application.getContext());
                if (server.isPresent()) {
                    if (endpoint == null)
                        endpoint = server.get().getEndpoint(handle, targetConfig.getCorpNo()).orElse(null);
                    if (token == null)
                        token = server.get().getToken(handle, targetConfig.getCorpNo()).orElse(null);
                }
            }
            if (Utils.isEmpty(endpoint))
                throw new RuntimeException("endpoint 不允许为空");
            return RemoteService.call(endpoint, token, key, dataIn);
        } else {
            ServerConfigImpl server = RemoteService.getServerConfig(Application.getContext())
                    .orElseThrow(() -> new RuntimeException("无法获取到有效的微服务配置 ServerConfigImpl"));
            String endpoint = server.getEndpoint(handle, targetConfig.getCorpNo()).orElse(null);
            String token = server.getToken(handle, targetConfig.getCorpNo()).orElse(null);
            return call(endpoint, token, key, dataIn);
        }
    }

    public static Optional<ServerConfigImpl> getServerConfig(ApplicationContext context) {
        if (context != null) {
            try {
                return Optional.of(context.getBean(ServerConfigImpl.class));
            } catch (NoSuchBeanDefinitionException e) {
                log.warn("微服务异常：未找到实现 ServerConfigImpl 的类");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return Optional.empty();
    }
}
