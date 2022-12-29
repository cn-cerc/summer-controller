package cn.cerc.mis.queue;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

public abstract class AbstractObjectQueue<T extends CustomMessageData> extends AbstractQueue {
    private static final Logger log = LoggerFactory.getLogger(AbstractObjectQueue.class);

    public abstract Class<T> getClazz();

    public String append(IHandle handle, T data) {
        if (Utils.isEmpty(data.getToken()))
            data.setToken(handle.getSession().getToken());
        if (!data.validate())
            throw new RuntimeException(String.format("[%s]数据不符合消息队列要求，无法发送！", this.getClazz().getSimpleName()));
        return super.push(new Gson().toJson(data));
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        T data = new Gson().fromJson(message, getClazz());
        try (TaskHandle handle = new TaskHandle()) {
            if (!Utils.isEmpty(data.getToken()))
                handle.getSession().loadToken(data.getToken());
            var result = this.execute(handle, data);
            // 非Sqlmq队列执行失败后，将其插入到Sqlmq中继续执行
            if (repushOnError && !result && this.getDelayTime() > 0 && this.getService() != QueueServiceEnum.Sqlmq) {
                super.pushToSqlmq(message);
                return true;
            }
            return result;
        }
    }

    public T addItem() {
        T result = null;
        try {
            result = getClazz().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public abstract boolean execute(IHandle handle, T entity);

//
//    public boolean receive(OnObjectMessage<T> event) {
//        QueueConsumer consumer = QueueConsumer.getConsumer(this.getTopic(), this.getTag());
//        return consumer.receive(message -> event.execute(new Gson().fromJson(message, getClazz())));
//    }
}
