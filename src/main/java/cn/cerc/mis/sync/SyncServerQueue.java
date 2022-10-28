package cn.cerc.mis.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.ISession;
import cn.cerc.db.queue.QueueConfig;
import cn.cerc.db.queue.QueueConsumer;
import cn.cerc.db.queue.QueueServer;
import cn.cerc.mis.core.SystemBuffer.SyncServer;

public class SyncServerQueue implements ISyncServer {

    private static final Logger log = LoggerFactory.getLogger(SyncServerQueue.class);

    private SyncServer pushFrom;
    private SyncServer pushTo;

    private SyncServer popFrom;
    private SyncServer popTo;

    public void initPushQueue(SyncServer pushFrom, SyncServer pushTo) {
        this.pushFrom = pushFrom;
        this.pushTo = pushTo;
    }

    public void initPopQueue(SyncServer popFrom, SyncServer popTo) {
        this.popFrom = popFrom;
        this.popTo = popTo;
    }

    @Override
    public void push(ISession session, DataRow record) {
        if (pushFrom == null)
            throw new RuntimeException("pushFrom is null");
        if (pushTo == null)
            throw new RuntimeException("pushTo is null");

        // 数据写入队列
        String topic = pushFrom.name().toLowerCase() + "-to-" + pushTo.name().toLowerCase();
        QueueServer.append(topic, QueueConfig.tag, record.toString());
    }

    @Override
    public void repush(ISession session, DataRow record) {
        throw new RuntimeException("this is repush disabled.");
    }

    @Override
    public int pop(ISession session, IPopProcesser popProcesser, int maxRecords) {
        if (popFrom == null)
            throw new RuntimeException("popFrom is null");
        if (popTo == null)
            throw new RuntimeException("popTo is null");

        // 取出数据队列
        String topic = popFrom.name().toLowerCase() + "-to-" + popTo.name().toLowerCase();
        QueueConsumer.create(topic, QueueConfig.tag, body -> {
            if (body == null) {
                return true;
            }

            DataRow record = new DataRow();
            record.setJson(body);
            try {
                if (!popProcesser.popRecord(session, record, true)) {
                    log.error("{} 处理失败，请检查数据源和帐套信息 {}", body);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error(record.toString(), e);
            }
            return false;
        });
        return maxRecords;
    }

}
