package cn.cerc.mis.queue;

import cn.cerc.db.core.IHandle;

/**
 * 通知型消息队列
 * 
 * @author 张弓 2023/7/8
 *
 */
public class QueueNoticeMessage extends AbstractObjectQueue<NoticeMessageData> {
//    private static final Logger log = LoggerFactory.getLogger(QueueNotice.class);

    @Override
    public Class<NoticeMessageData> getClazz() {
        return NoticeMessageData.class;
    }

    @Override
    public boolean execute(IHandle handle, NoticeMessageData data) {
        return data.send(handle);
    }

    public static String send(IHandle handle, String toUser, String subject, String content) {
        var data = new NoticeMessageData();
        data.setToken(handle.getSession().getToken());
        data.setToUser(toUser);
        data.setSubject(subject);
        data.setContent(content);
        return new QueueNoticeMessage().appendToLocal(handle, data);
    }
}
