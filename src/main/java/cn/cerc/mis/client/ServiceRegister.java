package cn.cerc.mis.client;

import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.zk.ZkNode;
import cn.cerc.mis.register.center.ApplicationEnvironment;

@Component
public class ServiceRegister implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegister.class);

    @Autowired
    private ServerConfigImpl config;

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
        var zk = ZkNode.get().server();

        var rootPath = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                ServerConfig.getAppOriginal());
        var groupPath = rootPath + "/" + (Utils.isEmpty(config.group()) ? "undefined" : config.group());

        // 建立永久结点
        if (!zk.exists(rootPath))
            zk.create(rootPath, config.extranet(), CreateMode.PERSISTENT);

        // 建立临时子结点
        String hostname = ApplicationEnvironment.hostname();
        DataRow node = DataRow.of("host", config.intranet(), "hostname", hostname, "time", new Datetime());
        zk.create(groupPath, node.json(), CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /**
     * 
     * @return 返回可用的服务地址
     */
    public ServiceSiteRecord getServiceHost(String industry) {
        var rootPath = String.format("/%s/%s/%s/host", ServerConfig.getAppProduct(), ServerConfig.getAppVersion(),
                industry);
        var zk = ZkNode.get().server();
        var list = zk.getNodes(rootPath);
        if (list.size() > 0) {
            log.debug("{} 有找到可用节点：{}", industry, list.size());
            var nodeKey = list.get(new Random().nextInt(list.size()));
            var nodeValue = zk.getValue(rootPath + "/" + nodeKey);
            var node = new DataRow().setJson(nodeValue);
            return new ServiceSiteRecord(true, industry, node.getString("host"));
        } else {
            var website = zk.getValue(rootPath);
            log.warn("{} 没有有找到可用节点，改使用外网调用：{}", industry, website);
            return new ServiceSiteRecord(false, industry, website);
        }
    }

    public ServerConfigImpl getConfig() {
        return config;
    }

    public void setConfig(ServerConfigImpl config) {
        this.config = config;
    }
}
