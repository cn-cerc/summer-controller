package cn.cerc.mis.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    public static Properties properties() {
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
            InetAddress address = ApplicationEnvironment.getExtranetAddress();
            hostip = address.getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            log.error(e.getMessage(), e);
        }
        return hostip;
    }

    /**
     * 应用主机分组
     * <p>
     * docker run <br>
     * --env DOCKER_GROUP=$group \
     */
    public static String group() {
        // docker 容器分组
        String group = System.getenv("DOCKER_GROUP");
        if (!Utils.isEmpty(group))
            return group;
        return ServerConfig.getInstance().getProperty("application.group", "undefined");
    }

    /**
     * 获取本机IP的对外的网卡信息
     */
    public static InetAddress getExtranetAddress() throws UnknownHostException, SocketException {
        InetAddress address = null;
        Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface face : Collections.list(faces)) {
            // 过滤调 docker 生成的网卡
            String name = face.getName().toLowerCase();
            if (name.startsWith("docker") || name.startsWith("br-"))
                continue;

            // 获取该网卡接口下的所有IP地址列表
            Enumeration<InetAddress> items = face.getInetAddresses();
            while (items.hasMoreElements()) {
                InetAddress element = items.nextElement();
                // 排除 loopback 回环类型地址
                if (element.isLoopbackAddress())
                    continue;

                // 如果是 site-local 地址，它就是我们要找的地址
                if (element.isSiteLocalAddress())
                    return element;

                // 若不是site-local地址 那就记录下该地址当作候选
                if (address == null)
                    address = element;
            }
        }

        // 如果排除回环地之外无其它地址了，那就回退到原始方案吧
        return address == null ? InetAddress.getLocalHost() : address;
    }

    /**
     * 应用主机端口
     * <p>
     * docker run <br>
     * --env DOCKER_HOST_IP=`hostname -I | awk '{print $1}'` \ <br>
     * --env DOCKER_HOST_PORT=$port \
     */
    public static String hostPort() {
        // docker 容器内就先读取环境变量，否则读取到的是内网地址，此变量需要建立容器时手动设置
        String httpPort = System.getenv("DOCKER_HOST_PORT");
        if (!Utils.isEmpty(httpPort))
            return httpPort;

        try {
            // Tomcat配置文件路径
            String catalinaHome = System.getProperty("catalina.home");
            String serverXmlPath = catalinaHome + File.separator + "conf" + File.separator + "server.xml";

            // 创建DOM解析器
            Document doc;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                doc = factory.newDocumentBuilder().parse(serverXmlPath);
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            // 查找Connector元素
            NodeList connectors = doc.getElementsByTagName("Connector");
            for (int i = 0; i < connectors.getLength(); i++) {
                Element connector = (Element) connectors.item(i);
                String protocol = connector.getAttribute("protocol");
                if (protocol.startsWith("HTTP")) {
                    httpPort = connector.getAttribute("port");
                    break;
                }
            }
            return httpPort;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "unknown";
        }
    }

}
