package cn.cerc.mis.log;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.Curl;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

@Component
public class QueueJayunLog extends AbstractQueue {

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
        JayunLogData logData = new Gson().fromJson(message, JayunLogData.class);
        Curl curl = new Curl();
        curl.doPost("http://127.0.0.1:8081/public/log-api", message);
        System.err.println(logData.getProject() + "===" + logData.getMessage());
        return true;
    }


}
