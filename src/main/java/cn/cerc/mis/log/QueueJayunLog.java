package cn.cerc.mis.log;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

@Component
public class QueueJayunLog extends AbstractQueue {
    private static final ClassConfig config = new ClassConfig();
    public static final String prefix = "qc";

    public QueueJayunLog() {
        super();
        this.setService(QueueServiceEnum.RabbitMQ);
        this.setPushMode(true);
    }

    public String push(JayunLogData logData) {
        try {
            return super.push(new Gson().toJson(logData));
        } catch (Exception e) {
            return "error";
        }
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        String site = config.getString(key("api.log.site"), "");
        if (Utils.isEmpty(site))
            return true;

        JayunLogData data = new Gson().fromJson(message, JayunLogData.class);
        String profile = key(String.format("%s.log.token", data.getProject()));
        String token = config.getString(profile, "");
        if (Utils.isEmpty(token)) {
            System.err.println(String.format("%s 项目日志配置 %s 为空", data.getProject(), profile));
            return true;
        }
        data.setToken(token);

        String json = new Gson().toJson(data);
        executor.submit(() -> {
            try {
                Curl curl = new Curl();
                String response = curl.doPost(site, json);
                if (Utils.isEmpty(response))
                    System.err.println(String.format("site %s, json %s", site, json));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        });
        return true;
    }

    public static String key(String key) {
        return String.format("%s.%s", prefix, key);
    }

}
