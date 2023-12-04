package cn.cerc.mis.queue;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;
import cn.cerc.local.tool.JsonTool;
import cn.cerc.mis.client.CorpConfigImpl;
import cn.cerc.mis.client.RemoteService;
import cn.cerc.mis.client.ServerConfigImpl;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.log.JayunLogParser;

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
        if (!data.validate()) {
            throw new RuntimeException(String.format("[%s] 数据不符合消息队列要求，无法发送！ [corpNo] %s, [data] %s",
                    this.getClazz().getSimpleName(), handle.getCorpNo(), JsonTool.toJson(data)));
        }
        return super.push(new Gson().toJson(data));
    }

    public String appendToRemote(IHandle handle, CorpConfigImpl config, T data) {
        Objects.requireNonNull(config);
//        config.getOriginal().ifPresent(value -> this.setOriginal(value));

        if (!config.isLocal()) {
            ServerConfigImpl serverConfig = Application.getBean(ServerConfigImpl.class);
            if (serverConfig != null) {
                try {
                    serverConfig.getIndustry(handle, config.getCorpNo()).ifPresent(value -> this.setOriginal(value));
                } catch (ServiceException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        if (!Utils.isEmpty(config.getCorpNo())) {
            Optional<ServerConfigImpl> serviceConfig = RemoteService.getServerConfig(Application.getContext());
            if (serviceConfig.isPresent()) {
                Optional<String> remoteToken;
                try {
                    remoteToken = serviceConfig.get().getToken(handle, config.getCorpNo());
                } catch (ServiceException e) {
                    throw new RuntimeException(e.getMessage());
                }
                if (remoteToken.isPresent())
                    data.setToken(remoteToken.get());
            }
        }

        if (!data.validate()) {
            throw new RuntimeException(String.format("[%s] 数据不符合消息队列要求，无法发送！ [corpNo] %s, [data] %s",
                    this.getClazz().getSimpleName(), handle.getCorpNo(), JsonTool.toJson(data)));
        }
        return super.push(new Gson().toJson(data));
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        T data = new Gson().fromJson(message, getClazz());
        if (data == null)
            return true;
        try (TaskHandle handle = new TaskHandle()) {
            if (!Utils.isEmpty(data.getToken())) {
                this.repairToken(data.getToken());
                boolean loadToken = handle.getSession().loadToken(data.getToken());
                if (!loadToken) {
                    String error = String.format("已失效 %s，执行类 %s，消息体 %s", data.getToken(), this.getClass(), message);
                    JayunLogParser.warn(this.getClass(), new RuntimeException(error));
                    log.info(error);
                    return true;
                }
            }
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

    /**
     * 执行消息消费
     * 
     * @param handle
     * @param entity
     * @return 消息消费成功否
     */
    public abstract boolean execute(IHandle handle, T entity);

//
//    public boolean receive(OnObjectMessage<T> event) {
//        QueueConsumer consumer = QueueConsumer.getConsumer(this.getTopic(), this.getTag());
//        return consumer.receive(message -> event.execute(new Gson().fromJson(message, getClazz())));
//    }

}
