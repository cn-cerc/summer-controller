package cn.cerc.mis.queue;

import cn.cerc.db.core.Utils;

public class CustomMessageObject {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 
     * @return 检查各项数据是否符合消息队列要求
     */
    public boolean validate() {
        return !Utils.isEmpty(token);
    }
}
