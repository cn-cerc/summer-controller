package cn.cerc.mis.client;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
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
import cn.cerc.db.zk.ZkNode;
import cn.cerc.db.zk.ZkServer;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.register.center.ApplicationEnvironment;

@Component
public class ServiceRegister implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Watcher {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegister.class);
    private static final ClassConfig config = new ClassConfig(ServerConfig.class, SummerMIS.ID);
    private ApplicationContext context;
//
//    private static final String ROOT_PATH = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(),
//            ServerConfig.getAppVersion(), ServerConfig.getAppOriginal());

    /**
     * 子节点信息列表
     */
    private static final Map<String, Map<String, String>> intranetItems = new ConcurrentHashMap<>();

    /**
     * 负载均衡计数器
     */
    private static final AtomicInteger atomic = new AtomicInteger();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            try {
                register();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void register() throws KeeperException, InterruptedException {
        if (context == null) {
            log.error("applicationContext is null");
            return;
        }

        // 取得内网节点地址
        var port = config.getProperty("application.port", ApplicationEnvironment.hostPort());
        var ip = ApplicationEnvironment.hostIP();
        var host = String.format("http://%s:%s", ip, port);
        var myIntranet = config.getString("application.localhost", host);
        // 取得外网节点域名
        var myExtranet = config.getProperty("application.website", "http://localhost:80");
        // 主机分组代码: 相同的主机之间，使用 intranet 调用，否则使用 extranet 调用
        var myGroup = config.getProperty("application.group", "undefined");

        var zk = ZkNode.get().server();
        // 建立永久结点
        var rootPath = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                ServerConfig.getAppOriginal());
        ZkNode.get().getNodeValue(rootPath, () -> myExtranet);

        // 建立临时子结点
        var groupPath = rootPath + "/" + myGroup;
        String hostname = ApplicationEnvironment.hostname();
        DataRow node = DataRow.of("host", myIntranet, "hostname", hostname, "time", new Datetime());
        zk.create(groupPath, node.json(), CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /**
     * 
     * @return 返回可用的服务地址
     */
    public ServiceSiteRecord getServiceHost(String industry) {
        String path = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                industry);
        ZkServer server = ZkNode.get().server();
        Map<String, String> map;
        try {
            map = intranetItems.get(industry);
            if (map == null) {
                var list = server.client().getChildren(path, this);
                map = new ConcurrentHashMap<String, String>();
                for (var nodeKey : list) {
                    var nodeValue = server.getValue(path + "/" + nodeKey);
                    map.put(nodeKey, nodeValue);
                }
                intranetItems.put(industry, map);
            }
            if (map.size() > 0) {
                log.debug("{} 有找到可用节点：{}", industry, map.size());
                var list = map.keySet().toArray();
                var nodeKey = list[new Random().nextInt(intranetItems.size())];
                var nodeValue = map.get(nodeKey);
                var node = new DataRow().setJson(nodeValue);
                return new ServiceSiteRecord(true, industry, node.getString("intranet"));
            } else {
                var extranet = ZkNode.get().getNodeValue(path, () -> "");
                log.warn("{} 没有有找到可用节点，改使用外网调用：{}", industry, extranet);
                return new ServiceSiteRecord(false, industry, extranet);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;

    }

    @Override
    public void process(WatchedEvent event) {
        var server = ZkNode.get().server();
        String path = event.getPath();
        try {
            var client = server.client();
            log.info("watch path: {}", path);
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                Stat stat = client.exists(path, this);
                if (stat != null) {
                    var list = server.client().getChildren(path, this);
                    var map = new ConcurrentHashMap<String, String>();
                    for (var nodeKey : list) {
                        var nodeValue = server.getValue(path + "/" + nodeKey);
                        map.put(nodeKey, nodeValue);
                    }
                    intranetItems.put(path, map);
                } else
                    intranetItems.remove(path);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
//        if (!path.contains(ROOT_PATH))
//            return;
//
//        // 节点信息被删除，清空缓存列表
//        if (event.getType() == Event.EventType.NodeDeleted) {
//            return;
//        }

//        // 子节点列表有变化，重载缓存列表
//        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
//            var value = server.getValue(node);
//            if (value != null) {
//                intranetItems.put(node, value);
//                log.warn("节点 {} 值变更为 {}", node, value);
//                server.watch(node, this); // 继续监视
//            } else {
//                log.error("节点 {} 不应该找不到！！！", node);
//            }
//        } else if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
//            intranetItems.remove(node);
//            log.debug("节点 {} 已被删除！", node);
//        }

    }
}
