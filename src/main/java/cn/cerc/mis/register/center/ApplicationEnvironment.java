package cn.cerc.mis.register.center;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;

/**
 * 应用环境变量加载和配置列表
 */
public class ApplicationEnvironment {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEnvironment.class);

    /**
     * 获取系统的环境变量
     */
    public static Map<String, String> getenv() {
        return System.getenv();
    }

    /**
     * 获取虚拟机 系统属性
     */
    public static Properties properteis() {
        return System.getProperties();
    }

    /**
     * 应用主机名称
     */
    public static String hostname() {
        String hostname;
        try {
            InetAddress inet = InetAddress.getLocalHost();
            hostname = inet.getHostName();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
            hostname = "";
        }
        return hostname;
    }

    /**
     * 应用主机地址
     * <p>
     * docker run <br>
     * --env DOCKER_HOST_IP=`hostname -I | awk '{print $1}'` \ <br>
     * --env DOCKER_HOST_PORT=$port \
     */
    public static String hostIP() {
        // docker 容器内就先读取环境变量，否则读取到的是内网地址，此变量需要建立容器时手动设置
        String hostip = System.getenv("DOCKER_HOST_IP");
        if (!Utils.isEmpty(hostip))
            return hostip;

        try {
            InetAddress inet = InetAddress.getLocalHost();
            hostip = inet.getHostAddress();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        return hostip;
    }

    /**
     * 获取外网服务IP
     * 
     * @return
     */
    public static Optional<String> networkIP() {
        ServerConfig config = ServerConfig.getInstance();
        String httpPort = config.getProperty("application.external.svc");
        return Optional.ofNullable(httpPort);
    }

    /**
     * 应用主机端口
     * <p>
     * docker run <br>
     * --env DOCKER_HOST_IP=`hostname -I | awk '{print $1}'` \ <br>
     * --env DOCKER_HOST_PORT=$port \
     */
    public static Optional<String> hostPort() {
        // docker 容器内就先读取环境变量，否则读取到的是内网地址，此变量需要建立容器时手动设置
        String httpPort = System.getenv("DOCKER_HOST_PORT");
        if (!Utils.isEmpty(httpPort))
            return Optional.of(httpPort);
        // 用于开发环境使用
        ServerConfig config = ServerConfig.getInstance();
        httpPort = config.getProperty("app.port");
        if (!Utils.isEmpty(httpPort))
            return Optional.of(httpPort);
        return Optional.empty();
    }

}
