package cn.cerc.mis.log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(QueueJayunLog.class);
    // FIXME 如果记录INFO日志，会导致循环调用
    private static final ClassConfig config = new ClassConfig();
    public static final String prefix = "qc";

    // 创建一个缓存线程池，在必要的时候在创建线程，若线程空闲60秒则终止该线程
    public static final ExecutorService pool = Executors.newCachedThreadPool();

    public QueueJayunLog() {
        super();
        this.setService(QueueServiceEnum.RabbitMQ);
        this.setPushMode(true);
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
        String token = config.getString(key(String.format("%s.log.token", data.getProject())), "");
        if (Utils.isEmpty(token)) {
            log.warn("项目 {} 获取日志等级 {} token为空", data.getProject(), data.getLevel());
            return true;
        }
        data.setToken(token);
        String json = new Gson().toJson(data);
        pool.submit(() -> {
            try {
                Curl curl = new Curl();
                String response = curl.doPost(site, json);
                System.out.println(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    public static String key(String key) {
        return String.format("%s.%s", prefix, key);
    }

}
