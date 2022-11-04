package cn.cerc.mis.queue;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;

public abstract class AbstractObjectQueue<T extends CustomMessageObject> extends AbstractQueue {
    private static final Logger log = LoggerFactory.getLogger(AbstractObjectQueue.class);

    public abstract Class<T> getClazz();

    public String sendMessage(IHandle handle, T data) {
        if (Utils.isEmpty(data.getToken()))
            data.setToken(handle.getSession().getToken());
        return super.sendMessage(new Gson().toJson(data));
    }

    @Override
    public boolean consume(String message) {
        T data = new Gson().fromJson(message, getClazz());
        try (TaskHandle handle = new TaskHandle()) {
            handle.getSession().loadToken(data.getToken());
            return this.execute(handle, data);
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
