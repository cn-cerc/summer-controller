package cn.cerc.mis.queue;

import cn.cerc.db.core.DataCell;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.queue.AbstractQueue;

public abstract class AbstractDataRowQueue extends AbstractQueue {
    /**
     * 生产者投放消息
     */
    protected String push(IHandle handle, DataRow dataRow) {
        dataRow.setValue("token", handle.getSession().getToken());
        return super.push(dataRow.json());
    }

    @Override
    public final boolean consume(String message) {
        var data = new DataRow().setJson(message);
        try (TaskHandle handle = new TaskHandle()) {
            if (data.has("token")) {
                // 临时恢复token，由队列自己实现此方法，设置Redis缓存
                this.repairToken(data.getString("token"));
                handle.getSession().loadToken(data.getString("token"));
                DataCell corpNo = data.bind("corp_no_");// 执行器的目标帐套
                DataCell userCode = data.bind("user_code_");
                if (corpNo.hasValue())
                    handle.buildSession(corpNo.getString(), userCode.getString());
            }
            return this.execute(handle, data);
        }
    }

    protected void repairToken(String token) {

    }

    public abstract boolean execute(IHandle handle, DataRow data);

//    public boolean receive(OnMessageDataRow event) {
//        QueueConsumer consumer = new QueueConsumer();
//        return consumer.receive("tempGroup", this.getTopic(), this.getTag(), data -> event.execute(new DataRow().setJson(data)));
//    }

}
