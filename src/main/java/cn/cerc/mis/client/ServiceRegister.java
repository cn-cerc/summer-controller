package cn.cerc.mis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

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
    private static final ClassConfig config = new ClassConfig(ServiceRegister.class, SummerMIS.ID);
    private ApplicationContext context;

    /**
     * 主机分组代码: 相同的主机之间，使用 intranet 调用，否则使用 extranet 调用
     */
    public static final String myGroup = config.getProperty("application.group", "undefined");
    /**
     * 取得外网节点域名
     */
    public static final String extranet = config.getProperty("application.extranet", "http://127.0.0.1");
    /**
     * 内网节点信息列表
     */
    private static final Map<String, Map<String, String>> intranets = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            try {
                register();
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void register() throws KeeperException, InterruptedException {
        if (context == null) {
            log.error("applicationContext is null");
            return;
        }

        // 取得内网节点信息
        String port = config.getProperty("application.port", ApplicationEnvironment.hostPort());
        String ip = ApplicationEnvironment.hostIP();
        String host = String.format("http://%s:%s", ip, port);
        String intranet = config.getString("application.intranet", host);

        // 建立永久结点
        ZkServer server = ZkNode.get().server();
        String root = buildPath(ServerConfig.getAppOriginal());
        ZkNode.get().getNodeValue(root, () -> extranet);

        // 建立临时子结点
        String hostname = ApplicationEnvironment.hostname();
        String groupPath = root + "/" + myGroup + "-";
        DataRow node = DataRow.of("intranet", intranet, "hostname", hostname, "time", new Datetime());
        server.create(groupPath, node.json(), CreateMode.EPHEMERAL_SEQUENTIAL);

        // 注册Watcher，监听目录节点的子节点变化
        server.client().getChildren(root, this);
    }

    /**
     * @return 返回可用的服务地址
     */
    public ServiceSiteRecord getServiceHost(String industry) {
        String root = buildPath(industry);
        ZkServer server = ZkNode.get().server();
        Map<String, String> items = intranets.get(root);
        try {
            if (items == null) {
                List<String> list = server.client().getChildren(root, this);
                items = new ConcurrentHashMap<String, String>();
                for (String nodeKey : list) {
                    if (nodeKey.startsWith(myGroup)) {
                        String nodeValue = server.getValue(root + "/" + nodeKey);
                        items.put(nodeKey, nodeValue);
                    }
                }
                intranets.put(root, items);
            }

            if (items != null && items.size() > 0) {
                log.debug("{} 行业找到可用节点 {}", industry, items.size());
                List<String> list = new ArrayList<>(items.keySet());
                String nodeKey = list.get(new Random().nextInt(items.size()));
                String nodeValue = items.get(nodeKey);
                DataRow node = new DataRow().setJson(nodeValue);
                return new ServiceSiteRecord(true, industry, node.getString("intranet"));
            } else {
                String extranet = ZkNode.get().getNodeValue(root, () -> "");
                log.warn("{} 行业未找到可用节点，改使用外网调用{}", industry, extranet);
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
        String path = event.getPath();
        try {
            ZkServer server = ZkNode.get().server();
            ZooKeeper client = server.client();
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                Stat stat = client.exists(path, false);
                if (stat != null) {
                    List<String> list = server.client().getChildren(path, false);
                    Map<String, String> map = new ConcurrentHashMap<>();
                    for (String nodeKey : list) {
                        if (nodeKey.startsWith(myGroup)) {
                            String nodeValue = server.getValue(path + "/" + nodeKey);
                            if (nodeValue != null)
                                map.put(nodeKey, nodeValue);
                        }
                    }
                    intranets.put(path, map);
                    log.info("{} 子节点变化 {}", path, new Gson().toJson(map));
                } else {
                    intranets.remove(path);
                    log.info("{} 子节点移除", path);
                }
            }
            log.info("节点内存缓存 {}", new Gson().toJson(intranets));
            // 注册Watcher，继续监听目录节点的子节点变化
            server.client().getChildren(path, this);
        } catch (KeeperException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String buildPath(String industry) {
        String path = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                industry);
        log.debug("行业 {} -> 节点 {}", industry, path);
        return path;
    }

}
