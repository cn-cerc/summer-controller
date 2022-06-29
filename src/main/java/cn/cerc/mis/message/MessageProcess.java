package cn.cerc.mis.message;

import cn.cerc.mis.queue.AsyncService;

// TODO 增加对应的进度表示中文说明
public enum MessageProcess {

    stop,
    wait,
    working,
    ok,
    error,
    finish;

    public String getTitle() {
        return AsyncService.getProcessTitle(this.ordinal());
    }

    public static MessageProcess getItem(int val) {
        MessageProcess value = null;
        for (MessageProcess item : values()) {
            if (item.ordinal() == val) {
                value = item;
                break;
            }
        }
        if (value == null)
            throw new RuntimeException(String.format("不支持的消息进度 %s", val));
        return value;
    }

}
