package cn.cerc.mis.log;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

@Component
public class QueueJayunLog extends AbstractQueue {
    private static final ClassConfig config = new ClassConfig();
    public static final String prefix = "jayun";

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
        if (ServerConfig.isServerDevelop())
            return true;
        String site = config.getString(key("api.log.site"), "");
        if (Utils.isEmpty(site))
            return true;
        JayunLogData data = new Gson().fromJson(message, JayunLogData.class);
        String token = config.getString(key(String.format("%s.log.%s.token", data.getProject(), data.getLevel())), "");
        if (Utils.isEmpty(token))
            return true;
        data.setToken(token);
        message = new Gson().toJson(data);
        try {
            Curl curl = new Curl();
            curl.doPost(site, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static String key(String key) {
        return String.format("%s.%s", prefix, key);
    }

}
