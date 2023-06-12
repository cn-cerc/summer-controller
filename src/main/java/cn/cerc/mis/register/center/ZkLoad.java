package cn.cerc.mis.register.center;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.zk.ZkServer;

public class ZkLoad implements Watcher {

    private static final String POINTS = "/points";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    private static final Logger log = LoggerFactory.getLogger(ZkLoad.class);

    private volatile Map<String, List<ServerInfo>> serverMap = null;
    private static ZkLoad instance = new ZkLoad();
    private String rootPath;
    private String currentNodePath = null;
    //
    private String currentWanIp = null;
    private volatile Map<String, AtomicInteger> currentMap = null;
    private volatile Map<String, AtomicBoolean> watchedMap = null;

    private ZkLoad() {
        serverMap = new Hashtable<>();
        rootPath = String.format("/%s/%s/", ServerConfig.getAppProduct(), ServerConfig.getAppVersion());
        currentMap = new Hashtable<>();
        watchedMap = new Hashtable<>();
    }

    public static ZkLoad get() {
        return instance;
    }

    public Optional<String> getUrl(String module) {

        if (Utils.isEmpty(module))
            return Optional.empty();
        String path = rootPath + module + POINTS;
        List<ServerInfo> serverList = serverMap.get(path);
        if (serverList == null || watchedMap.get(module) == null || !watchedMap.get(module).get()) {
            currentMap.put(module, new AtomicInteger(0));
            ZkServer zk = ZkServer.get();
            if (!zk.exists(path)) {
                // 判断服务节点
                zk.create(path, "", CreateMode.PERSISTENT);
            }
            serverList = this.refreshChild(path);
            this.register();// 异常情况下，检查服务注册状态
            watchedMap.put(module, new AtomicBoolean(true));
        }
        if (serverList.size() > 0) {
            ServerInfo zkServer = serverList
                    .get(Math.abs(currentMap.get(module).getAndIncrement() % serverList.size()));
            if (zkServer.getLanPort() != null) {
                String server = null;
                if (!Utils.isEmpty(zkServer.getWanIp()) && !zkServer.getWanIp().equals(currentWanIp)) {
                    server = zkServer.getWanIp();
                    if (!server.toLowerCase().startsWith(HTTPS)) {
                        server = HTTPS + server;
                    }
                } else {
                    server = HTTP + String.format("%s:%s", zkServer.getLanIp(), zkServer.getLanPort());
                }
                return Optional.ofNullable(server);
            }
        }
        return Optional.empty();
    }

    // 刷新内存
    public List<ServerInfo> refreshChild(String path) {
        ZkServer zk = ZkServer.get();
        zk.client();
        List<ServerInfo> serverList = new ArrayList<>();
        List<String> childList = zk.getNodes(path, this);
        for (int i = 0; i < childList.size(); i++) {
            String content = zk.getValue(path + "/" + childList.get(i));
            ServerInfo server = new Gson().fromJson(content, ServerInfo.class);
            serverList.add(server);
        }
        log.info(childList.toString());
        serverMap.put(path, serverList);
        return serverList;
    }

    // 注册服务IP及端口
    public String register() throws RuntimeException {
        String lanIp = ApplicationEnvironment.hostIP();
        String lanPort = ApplicationEnvironment.hostPort();
        if (lanPort == null) {
            throw new RuntimeException("注册服务的端口为空 ，请配置参数 app.port ");
        }
        String original = ServerConfig.getAppOriginal();
        // 获取外网IP
        currentWanIp = ApplicationEnvironment.networkIP();
        String path = rootPath + original + POINTS;
        ZkServer zk = ZkServer.get();
        if (!zk.exists(path)) {
            zk.create(path, "", CreateMode.PERSISTENT);
        }
        currentNodePath = new StringBuffer(path).append("/").append(lanIp).append(":").append(lanPort).toString();
        if (!zk.exists(currentNodePath)) {
            ServerInfo server = new ServerInfo(lanIp, lanPort, original, currentWanIp);
            String content = new Gson().toJson(server);
            zk.create(currentNodePath, content, CreateMode.EPHEMERAL);
            log.info("注册服务 {}", currentNodePath);
        }
        return currentNodePath;
    }

    // 删除服务注册信息
    public void unRegister() {
        if (!Utils.isEmpty(currentNodePath)) {
            ZkServer zk = ZkServer.get();
            if (zk.exists(currentNodePath)) {
                zk.delete(currentNodePath);
                log.info("删除注册服务 {}", currentNodePath);
            }
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        // zk 路径
        String path = watchedEvent.getPath();
        // 判断是否建立连接
        Event.KeeperState keeperState = watchedEvent.getState();
        // 获取事件类型
        Event.EventType eventType = watchedEvent.getType();
        log.info("进入到 process() keeperState: {} , eventType: {} , path: {}", keeperState, eventType, path);
        try {
            if (Event.KeeperState.SyncConnected == keeperState) {
                if (Event.EventType.NodeChildrenChanged == eventType) {
                    this.refreshChild(path);
                }
            }
        } catch (Exception e) {
            watchedMap.forEach((module, watched) -> {
                watched.set(false);
            });
            log.error("监听zk异常", e);
        }
    }
}
