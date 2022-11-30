package cn.cerc.mis.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.ISession;
import cn.cerc.db.queue.OnStringMessage;
import cn.cerc.mis.core.Application;
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
//        String topic = pushFrom.name().toLowerCase() + "-to-" + pushTo.name().toLowerCase();
//        var queue = new QueueProducer(topic, QueueConfig.tag());
//        try {
//            queue.append(record.toString(), Duration.ZERO);
//        } catch (ClientException e) {
//            log.error(e.getMessage());
//            e.printStackTrace();
//        }
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
        if (Application.enableTaskService()) {
            OnStringMessage pull = body -> {
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
                    return false;
                }
            };
//            var consumer = new QueueConsumer();
//            consumer.addConsumer(topic, QueueConfig.tag(), pull);
//            consumer.startPush();
            return maxRecords;
        } else {
            return 0;
        }
    }

}
