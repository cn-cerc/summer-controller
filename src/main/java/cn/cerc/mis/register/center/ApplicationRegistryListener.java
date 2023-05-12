package cn.cerc.mis.register.center;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.zk.ZkServer;

@Component
public class ApplicationRegistryListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null)
            register();
    }

    private void register() {
        ZkServer server = ZkServer.get();
        ZooKeeper client = server.client();
        String appProduct = ServerConfig.getAppProduct();
        String appOriginal = ServerConfig.getAppOriginal();
        String appVersion = ServerConfig.getAppVersion();
        String appName = ServerConfig.getAppName();

        String path = String.format("/%s/%s/%s/%s", appProduct, appVersion, appOriginal, "points");
        try {
            if (client.exists(path, false) == null)
                server.create(path, "", CreateMode.PERSISTENT);

            String hostname = ApplicationEnvironment.hostname();
            String ip = ApplicationEnvironment.hostIP();
            String port = ApplicationEnvironment.hostPort();
            String value = String.join(" ", new Datetime().toString(), appName, hostname, ip, port);
            server.create(String.join("/", path, Utils.getGuid()), value, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
