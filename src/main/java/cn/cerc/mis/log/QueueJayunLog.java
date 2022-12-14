package cn.cerc.mis.log;

import com.google.gson.Gson;

import cn.cerc.db.queue.AbstractQueue;

public class QueueJayunLog extends AbstractQueue {

    public String push(JayunLogData logData) {
        return super.push(new Gson().toJson(logData));
    }

    @Override
    public boolean consume(String message) {
        JayunLogData logData = new Gson().fromJson(message, JayunLogData.class);
        System.out.println(logData.getMessage());
        return true;
    }

}
