package cn.cerc.mis.queue;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.Datetime.DateType;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlText;
import cn.cerc.db.queue.OnStringMessage;
import cn.cerc.db.queue.QueueServiceEnum;
import cn.cerc.db.queue.sqlmq.SqlmqServer;
import cn.cerc.db.redis.Redis;
import cn.cerc.mis.ado.EntityMany;
import cn.cerc.mis.ado.EntityOne;

public class SqlmqQueue implements IHandle {
    private static final Logger log = LoggerFactory.getLogger(SqlmqQueue.class);

    private String queue;
    private int delayTime = 0;
    private QueueServiceEnum service = QueueServiceEnum.Sqlmq;
    private ISession session;
    private String queueClass;

    public enum AckEnum {
        Read,
        Ok,
        Error;
    }

    public enum StatusEnum {
        Waiting,
        Working,
        Finish,
        Next,
        Invalid;
    }

    public SqlmqQueue() {
        this.session = SqlmqServer.get().getSession();
    }

    public SqlmqQueue(String queue) {
        this.session = SqlmqServer.get().getSession();
        this.queue = queue;
    }

    public void pop(int maximum, OnStringMessage onConsume) {
        SqlText sql = new SqlText(SqlServerType.Mysql);
        sql.add("select * from %s", SqlmqInfo.TABLE);
        sql.add("where ((status_=%d)", StatusEnum.Waiting.ordinal());
        sql.add("or (status_=%d and show_time_ <= '%s'))", StatusEnum.Next.ordinal(), new Datetime());
        sql.add("and service_=%s", QueueServiceEnum.Sqlmq.ordinal());
        sql.add("and queue_='%s'", this.queue);
        sql.setMaximum(1);
        EntityMany<SqlmqInfo> sqlmqInfo = EntityMany.open(this, SqlmqInfo.class, sql);
        try (Redis redis = new Redis()) {
            for (var row : sqlmqInfo) {
                consumeMessage(sqlmqInfo, redis, row, onConsume);
            }
        }
    }

    public void consumeMessage(EntityMany<SqlmqInfo> many, Redis redis, SqlmqInfo row, OnStringMessage onConsume) {
        Integer uid = row.getUID_();
        var lockKey = "sqlmq." + uid.toString();
        if (redis.setnx(lockKey, new Datetime().toString()) == 0)
            return;
        redis.expire(lockKey, 60 * 30);
        try {
            String content = "";
            boolean result = false;
            try {
                SqlQuery dataSet = many.dataSet();
                addLog(uid.longValue(), AckEnum.Read, content);
                many.updateAll(item -> {
                    item.setStatus_(StatusEnum.Working.ordinal());
                    item.setConsume_times_(dataSet.getInt("consume_times_") + 1);
                });
                result = onConsume.consume(row.getMessage_());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                content = e.getMessage();
            }
            addLog(uid.longValue(), result ? AckEnum.Ok : AckEnum.Error, content);

            if (result)
                many.updateAll(item -> item.setStatus_(StatusEnum.Finish.ordinal()));
            else
                many.updateAll(item -> {
                    item.setStatus_(StatusEnum.Next.ordinal());
                    item.setShow_time_(new Datetime().inc(DateType.Second, this.delayTime));
                });
        } finally {
            redis.del(lockKey);
        }
    }

    public String push(String message, String order) {
        SqlText sql = new SqlText(SqlServerType.Mysql);
        sql.add("select * from %s", SqlmqInfo.TABLE);
        sql.setMaximum(0);
        var sqlmqInfo = EntityOne.open(this, SqlmqInfo.class, sql);

        sqlmqInfo.orElseInsert(item -> {
            item.setQueue_(this.queue);
            item.setOrder_(order);
            item.setShow_time_(new Datetime());
            item.setMessage_(message);
            item.setConsume_times_(0);
            item.setStatus_(StatusEnum.Waiting.ordinal());
            item.setDelayTime_(delayTime);
            item.setService_(service.ordinal());
            item.setProduct_(ServerConfig.getAppProduct());
            item.setIndustry_(ServerConfig.getAppIndustry());
            item.setQueue_class_(this.queueClass);
        });
        return sqlmqInfo.dataSet().getString("UID_");
    }

    private void addLog(long queueId, AckEnum ack, String content) {
        SqlText sql = new SqlText(SqlServerType.Mysql);
        sql.add("select * from %s", SqlmqLog.TABLE);
        sql.setMaximum(0);
        var sqlmqLog = EntityOne.open(this, SqlmqLog.class, sql);

        sqlmqLog.orElseInsert(item -> {
            item.setQueue_id_((int) queueId);
            item.setAck_(ack.ordinal());
            item.setContent_(content);
            try {
                item.setIp_(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public void setService(QueueServiceEnum service) {
        this.service = service;
    }

    public QueueServiceEnum getService() {
        return service;
    }

    @Override
    public ISession getSession() {
        return this.session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    public String getQueueClass() {
        return queueClass;
    }

    public void setQueueClass(String queueClass) {
        this.queueClass = queueClass;
    }

}
