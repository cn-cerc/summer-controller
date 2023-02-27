package cn.cerc.mis.log;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.Curl;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;
import cn.cerc.db.zk.ZkNode;
import cn.cerc.db.zk.ZkServer;

@Component
public class QueueJayunLog extends AbstractQueue {

    public static final String prefix = "/jayun-test";
    private Map<String, String> items = new HashMap<>();

    public void initMap(String name) {
        if (ZkServer.get() == null)
            return;
        String[] levels = new String[] { "info", "warn", "error" };
        for (String level : levels) {
            String token = ZkNode.get()
                    .getNodeValue(String.format("%s/%s/log/%s", QueueJayunLog.prefix, name, level), () -> "");
            items.put(level, token);
        }
    }

    public QueueJayunLog() {
        super();
        this.setService(QueueServiceEnum.Redis);
    }

    public String push(JayunLogData logData) {
        return super.push(new Gson().toJson(logData));
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        // 本地开发不发送日志到测试平台
//        if (ServerConfig.isServerDevelop())
//            return true;
//        String site = ZkNode.get().getNodeValue(key("api-log"), () -> "");
        JayunLogData data = new Gson().fromJson(message, JayunLogData.class);
        if (items.isEmpty())
            initMap(data.getToken());
        data.setToken(items.get(data.getLevel()));
        if (data.getToken() == null)
            return true;
        String site = "http://127.0.0.1:8601/public/log";
        if (Utils.isEmpty(site))
            return true;
        try {
            Curl curl = new Curl();
            curl.doPost(site, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static String key(String key) {
        return String.format("%s/%s", prefix, key);
    }

}
