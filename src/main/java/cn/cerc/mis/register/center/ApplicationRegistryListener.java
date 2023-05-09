package cn.cerc.mis.register.center;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

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

        String path = String.format("/%s/%s/%s/%s", appProduct, appVersion, appOriginal, "points");
        try {
            if (client.exists(path, false) == null)
                server.create(path, "", CreateMode.PERSISTENT);

            InetAddress addr = InetAddress.getLocalHost();
            String ip = addr.getHostAddress();
            String hostname = addr.getHostName();
            String dockerIP = System.getenv("DOCKER_HOST");
            if (!Utils.isEmpty(dockerIP))
                ip = dockerIP;
            server.create(String.join("/", path, Utils.getGuid()), String.join("-", hostname, ip, "alive"),
                    CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
