package cn.cerc.mis.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.zk.ZkServer;

public class ServiceRegisterRecord {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegisterRecord.class);
    /**
     * 内网节点信息列表
     */
    private final Map<String, String> intranets = new ConcurrentHashMap<>();
    private final String rootPath;
    private final String groupPath;
    private String group;
    private boolean refreshing = false;

    protected ServiceRegisterRecord(String industry) {
        this.rootPath = ServiceRegister.buildRootPath(industry);
        this.groupPath = ServiceRegister.buildGroupPath(industry);
        this.refreshIntranets();// 初始化
        try {
            // 注册监听节点
            ZkServer.get().client().getChildren(rootPath, new RootChildrenNodeWatcher(this));
            ZkServer.get().watch(groupPath, new GroupNodeWatcher(this));
        } catch (KeeperException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 监听根节点下的子节点新增和删除
     * 
     * @param registerRecord
     */
    private record RootChildrenNodeWatcher(ServiceRegisterRecord registerRecord) implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            String path = event.getPath();
            if (event.getType() == EventType.NodeChildrenChanged) {
                log.debug("分组节点发生变更 {}", path);
                registerRecord.refreshIntranets();
            }
            try {
                ZkServer.get().client().getChildren(registerRecord.rootPath, this);
            } catch (KeeperException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 监听分组节点数据变更
     * 
     * @param registerRecord
     */
    private record GroupNodeWatcher(ServiceRegisterRecord registerRecord) implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            ZkServer server = ZkServer.get();
            String path = event.getPath();
            if (event.getType() == EventType.NodeDataChanged) {
                log.debug("分组节点发生变更 {}", path);
                registerRecord.refreshIntranets();
            }
            server.watch(path, this);
        }
    }

    /**
     * 监听子节点数据变更
     * 
     * @param registerRecord
     */
    private record IntranetsNodeWatcher(ServiceRegisterRecord registerRecord) implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            registerRecord.refreshing = true;
            try {
                ZkServer server = ZkServer.get();
                String path = event.getPath();
                String nodeKey = path.substring(registerRecord.rootPath.length() + 1);
                if (event.getType() == EventType.NodeDataChanged) {
                    if (registerRecord.intranets.containsKey(nodeKey)) {
                        synchronized (registerRecord.intranets) {
                            if (server.exists(path))
                                registerRecord.intranets.put(nodeKey, server.getValue(path));
                            else
                                registerRecord.intranets.remove(nodeKey);
                        }
                    }
                } else if (event.getType() == EventType.NodeDeleted) {
                    synchronized (registerRecord.intranets) {
                        registerRecord.intranets.remove(nodeKey);
                    }
                }
                server.watch(path, this);
            } finally {
                registerRecord.refreshing = false;
            }
        }
    }

    /**
     * 刷新内网节点信息
     */
    private void refreshIntranets() {
        refreshing = true;
        try {
            ZkServer server = ZkServer.get();
            if (!server.exists(groupPath))
                return;
            group = server.getValue(groupPath);

            List<String> list = server.getNodes(rootPath);
            synchronized (intranets) { // 将 intranets 上锁
                intranets.clear(); // 清空当前节点信息
                log.debug("{} {} 子节点：{}", groupPath, group, list);
                String startWith = group + "-";
                for (String nodeKey : list) {
                    if (nodeKey.startsWith(startWith)) {
                        String nodePath = rootPath + "/" + nodeKey;
                        String nodeValue = server.getValue(nodePath);
                        if (nodeValue != null) {
                            intranets.put(nodeKey, nodeValue);
                            server.watch(nodePath, new IntranetsNodeWatcher(this));
                        }
                    }
                }
            }
        } finally {
            refreshing = false;
        }
    }

    /**
     * 获取当前主机内存缓存中的内网节点信息
     */
    public List<String> getIntranets() {
        if (refreshing) {
            synchronized (intranets) {
                return List.copyOf(intranets.values());
            }
        }
        return List.copyOf(intranets.values());
    }

    public String getGroup() {
        return group;
    }
}
