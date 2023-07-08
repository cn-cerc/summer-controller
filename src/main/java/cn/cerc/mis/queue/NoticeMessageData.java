package cn.cerc.mis.queue;

import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.Application;

public class NoticeMessageData extends CustomMessageData {
    // 发送用户
    private String toUser;
    // 发送摘要
    private String subject;
    // 发送内容
    private String content;
    // 消息等级
    private NoticeLevelEnum level = NoticeLevelEnum.Low;

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NoticeLevelEnum getLevel() {
        return level;
    }

    public void setLevel(NoticeLevelEnum level) {
        this.level = level;
    }

    public boolean send(IHandle handle) {
        var context = Application.getContext();
        var impl = context.getBean(NoticeSenderImpl.class);
        return impl.send(handle, this);
    }
}
