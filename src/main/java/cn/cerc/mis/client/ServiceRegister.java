package cn.cerc.mis.client;

import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.register.center.ApplicationEnvironment;

@Component
public class ServiceRegister implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegister.class);
    private static final ClassConfig config = new ClassConfig(ServerConfig.class, SummerMIS.ID);
    private ApplicationContext context;

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
        if (!zk.exists(rootPath))
            zk.create(rootPath, myExtranet, CreateMode.PERSISTENT);

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

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;

    }
}
