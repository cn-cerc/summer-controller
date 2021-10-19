package cn.cerc.mis.other;

import com.google.gson.Gson;

import cn.cerc.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.SystemBuffer;
import redis.clients.jedis.Jedis;

public class TimeOut {
    private String project;
    private String corpNo;
    private String userCode;
    private String service;
    private long timer;
    private String dataIn;

    public TimeOut() {
        super();
    }

    public TimeOut(IHandle owner, DataSet dataIn, String funcCode, long totalTime) {
        String[] tmp = owner.getClass().getName().split("\\.");
        String service = tmp[tmp.length - 1] + "." + funcCode;

        this.setProject(ServerConfig.getAppName());
        this.setCorpNo(owner.getCorpNo());
        this.setUserCode(owner.getUserCode());
        this.setService(service);
        this.setTimer(totalTime);
        this.setDataIn(dataIn.toJson());

        String json = new Gson().toJson(this);
        try (Jedis redis = JedisFactory.getJedis()) {
            String key = MemoryBuffer.buildKey(SystemBuffer.Global.TimeOut);
            redis.lpush(key, json);
        }
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getCorpNo() {
        return corpNo;
    }

    public void setCorpNo(String corpNo) {
        this.corpNo = corpNo;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public long getTimer() {
        return timer;
    }

    public void setTimer(long timer) {
        this.timer = timer;
    }

    public String getDataIn() {
        return dataIn;
    }

    public void setDataIn(String dataIn) {
        this.dataIn = dataIn;
    }

}