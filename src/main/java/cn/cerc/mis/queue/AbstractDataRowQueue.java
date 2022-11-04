package cn.cerc.mis.queue;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.queue.AbstractQueue;

public abstract class AbstractDataRowQueue extends AbstractQueue {
    /**
     * 生产者投放消息
     */
    public String append(IHandle handle, DataRow dataRow) {
        dataRow.setValue("token", handle.getSession().getToken());
        return super.sendMessage(dataRow.json());
    }

    @Override
    public final boolean consume(String message) {
        var data = new DataRow().setJson(message);
        try (TaskHandle handle = new TaskHandle()) {
            if (data.has("token"))
                handle.getSession().loadToken(data.getString("token"));
            return this.execute(handle, data);
        }
    }

    public abstract boolean execute(IHandle handle, DataRow data);

//
//    public boolean receive(OnMessageDataRow event) {
//        QueueConsumer consumer = new QueueConsumer();
//        return consumer.receive("tempGroup", this.getTopic(), this.getTag(), data -> event.execute(new DataRow().setJson(data)));
//    }

}
