package cn.cerc.mis.log;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

// FIXME 后续删除
@Component
public class QueueJayunLog extends AbstractQueue {
    private static final ClassConfig config = new ClassConfig();
    public static final String prefix = "qc";

    public QueueJayunLog() {
        super();
        this.setService(QueueServiceEnum.RabbitMQ);
        this.setPushMode(true);
    }

    public String push(JayunLogBuilder logData) {
        try {
            return super.push(new Gson().toJson(logData));
        } catch (Exception e) {
            return "error";
        }
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        return true;
    }

    public static String key(String key) {
        return String.format("%s.%s", prefix, key);
    }

}
