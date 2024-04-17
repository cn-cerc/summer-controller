package cn.cerc.mis.client;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.zk.ZkNode;
import cn.cerc.db.zk.ZkServer;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.log.ApplicationEnvironment;

@Component
public class ServiceRegister implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegister.class);
    private static final ClassConfig config = new ClassConfig(ServiceRegister.class, SummerMIS.ID);
    private ApplicationContext context;

    /**
     * 取得外网节点域名
     */
    public static final String extranet = config.getProperty("application.extranet", "http://127.0.0.1");
    /**
     * 内部验证域名
     */
    public static final String extranet_validate = config.getProperty("application.extranet.validate",
            "http://127.0.0.1");
    /**
     * 内网节点信息列表
     */
    private static final Map<String, ServiceRegisterRecord> intranets = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (context == null) {
            log.error("applicationContext is null");
            return;
        }

        String original = ServerConfig.getAppOriginal();
        String rootPath = buildRootPath(original);
        String groupPath = buildGroupPath(original);
        // 建立永久父级结点
        ZkNode.get().getNodeValue(rootPath, () -> extranet);
        ZkNode.get().getNodeValue(groupPath, ApplicationEnvironment::group);
        intranets.computeIfAbsent(original, ServiceRegisterRecord::new);
    }

    public boolean register() {
        // 即使自己不注册节点也要监听根目录的变化
        if (ServerConfig.isServerGray()) {
            log.info("gray 环境下的不参与内网节点注册");
            return true;
        }
        if (ServerConfig.enableTaskService()) {
            if (ServerConfig.isServerAlpha()) {
                log.info("alpha 环境下的job不参与内网节点注册");
                return true;
            }
            if (ServerConfig.isServerMaster()) {
                log.info("main 环境下的job不参与内网节点注册");
                return true;
            }
        }

        String original = ServerConfig.getAppOriginal();
        // 取得内网节点信息
        String port = config.getProperty("application.port", ApplicationEnvironment.hostPort());
        String ip = ApplicationEnvironment.hostIP();
        String host = String.format("http://%s:%s", ip, port);
        // 建立临时内网子结点
        String hostname = ApplicationEnvironment.hostname();
        String intranet = config.getString("application.intranet", host);

        ZkServer server = ZkServer.get();
        String rootPath = buildRootPath(original);
        String groupPath = buildGroupPath(original);
        List<String> childNodes = server.getNodes(rootPath);
        for (String node : childNodes) {
            String path = rootPath + "/" + node;
            if (Objects.equals(path, groupPath))
                continue; // 跳过 group 节点
            String json = server.getValue(path);
            if (Utils.isEmpty(json))
                continue;
            // 判断自身是否已经注册过节点
            DataRow dataRow = new DataRow().setJson(json);
            if (Objects.equals(intranet, dataRow.getString("intranet")))
                return true;
        }

        String nodeKey = rootPath + "/" + ApplicationEnvironment.group() + "-";
        DataRow node = DataRow.of("intranet", intranet, "hostname", hostname, "time", new Datetime());
        ZkServer.get().create(nodeKey, node.json(), CreateMode.EPHEMERAL_SEQUENTIAL);
        return true;
    }

    /**
     * @return 返回可用的服务地址
     */
    public ServiceSiteRecord getServiceHost(String industry) {
        ServiceRegisterRecord registerRecord = intranets.computeIfAbsent(industry, ServiceRegisterRecord::new);
        List<String> items = registerRecord.getIntranets();
        if (!Utils.isEmpty(items)) {
            log.debug("{} 行业找到可用节点 {}", industry, items.size());
            String nodeValue = items.get(new Random().nextInt(items.size()));
            DataRow node = new DataRow().setJson(nodeValue);
            return new ServiceSiteRecord(true, industry, node.getString("intranet"));
        } else {
            String extranet = ZkNode.get().getNodeValue(buildRootPath(industry), () -> "");
            if (!ServerConfig.isServerDevelop())
                log.warn("{} 行业未找到可用节点，改使用外网调用 {}", industry, extranet);
            return new ServiceSiteRecord(false, industry, extranet);
        }
    }

    public static String buildRootPath(String industry) {
        String path = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                industry);
        log.debug("行业 {} -> 节点 {}", industry, path);
        return path;
    }

    public static String buildGroupPath(String industry) {
        String rootPath = buildRootPath(industry);
        String path = String.format("%s/group", rootPath);
        log.debug("行业 {} -> 节点 {}", industry, path);
        return path;
    }

    /**
     * 获取当前主机内存缓存中的内网节点信息
     */
    public Map<String, ServiceRegisterRecord> listNodes() {
        return intranets;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

}
