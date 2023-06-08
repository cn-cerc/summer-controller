package cn.cerc.mis.register.center;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    public static String networkIP() {
        ServerConfig config = ServerConfig.getInstance();
        String httpPort = config.getProperty("application.external.svc");
        return httpPort;
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
        
        //用于开发环境使用
        ServerConfig config = ServerConfig.getInstance();
        httpPort = config.getProperty("app.port");
        if (!Utils.isEmpty(httpPort))
            return httpPort;
        
        return null;
//        try {
//            // Tomcat配置文件路径
//            String catalinaHome = System.getProperty("catalina.home");
//            String serverXmlPath = catalinaHome + File.separator + "conf" + File.separator + "server.xml";
//
//            // 创建DOM解析器
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            Document doc = factory.newDocumentBuilder().parse(serverXmlPath);
//
//            // 查找Connector元素
//            NodeList connectors = doc.getElementsByTagName("Connector");
//            for (int i = 0; i < connectors.getLength(); i++) {
//                Element connector = (Element) connectors.item(i);
//                String protocol = connector.getAttribute("protocol");
//                if (protocol != null && protocol.startsWith("HTTP")) {
//                    httpPort = connector.getAttribute("port");
//                    break;
//                }
//            }
//            return httpPort;
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return "unknow";
//        }
    }

}
