package cn.cerc.mis.queue;

import com.google.gson.Gson;

import cn.cerc.db.queue.AbstractQueue;

public class CustomMessageObject {
    private transient AbstractQueue queue;
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AbstractQueue getQueue() {
        return queue;
    }

    public void setQueue(AbstractQueue queue) {
        this.queue = queue;
    }

    public String sendMessage() {
        return queue.sendMessage(new Gson().toJson(this));
    }
}
