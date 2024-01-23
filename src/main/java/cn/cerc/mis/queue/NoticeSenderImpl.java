package cn.cerc.mis.queue;

import cn.cerc.db.core.IHandle;

public interface NoticeSenderImpl {

    /**
     * 发送消息
     * 
     * @return 是否发送成功
     */
    boolean send(IHandle handle, NoticeMessageData data);

}
