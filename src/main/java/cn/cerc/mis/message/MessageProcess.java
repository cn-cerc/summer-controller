package cn.cerc.mis.message;

import cn.cerc.mis.queue.AsyncService;

// TODO 增加对应的进度表示中文说明
public enum MessageProcess {

    stop, wait, working, ok, error, finish;

    public String getTitle() {
        return AsyncService.getProcessTitle(this.ordinal());
    }

}
