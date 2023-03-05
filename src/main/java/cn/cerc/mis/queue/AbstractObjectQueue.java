package cn.cerc.mis.queue;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;
import cn.cerc.db.queue.TokenConfigImpl;

public abstract class AbstractObjectQueue<T extends CustomMessageData> extends AbstractQueue {
    private static final Logger log = LoggerFactory.getLogger(AbstractObjectQueue.class);

    public abstract Class<T> getClazz();

    @Deprecated
    public String append(IHandle handle, T data) {
        return this.appendToLocal(handle, data);
    }

    // Local 不需要传递 token，直接使用当前handle的令牌
    public String appendToLocal(IHandle handle, T data) {
        if (!Utils.isEmpty(data.getToken()))
            log.warn("{}.appendToLocal 代码编写不符合规范，请予改进", this.getClass().getName());
        else
            data.setToken(handle.getSession().getToken());
        if (!data.validate())
            throw new RuntimeException(String.format("[%s]数据不符合消息队列要求，无法发送！", this.getClazz().getSimpleName()));
        return super.push(new Gson().toJson(data));
    }

    public String appendToRemote(IHandle handle, TokenConfigImpl config, T data) {
        Objects.requireNonNull(config);
        config.setSession(handle.getSession());
        if (config.getToken().equals(handle.getSession().getToken()))
            throw new RuntimeException("远程token不得与当前token一致");

        if (config.getOriginal() != null)
            this.setOriginal(config.getOriginal());
        data.setToken(config.getToken());
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
