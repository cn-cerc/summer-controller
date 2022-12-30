package cn.cerc.mis.queue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.springframework.stereotype.Component;

import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.mis.ado.CustomEntity;

@Component
@Entity
@EntityKey(fields = { "queue_id_", "create_time_" })
@Table(name = SqlmqLog.TABLE, indexes = { @Index(name = "ix_queue_time", columnList = "queue_id_, create_time_") })
@SqlServer(type = SqlServerType.Mysql)
public class SqlmqLog extends CustomEntity {
    public static final String TABLE = "s_sqlmq_log";

    @Id
    @GeneratedValue
    @Column(length = 11, nullable = false)
    @Describe(name = "主键ID")
    private Integer UID_;

    @Column(length = 11, nullable = false)
    @Describe(name = "队列编号")
    private Integer queue_id_;

    @Column(length = 11, nullable = false)
    @Describe(name = "处理标记（0-读取 1-确认 2-失败）")
    private Integer ack_;

    @Column(nullable = false)
    @Describe(name = "日志内容")
    private String content_;

    @Column(nullable = false, columnDefinition = "datetime")
    @Describe(name = "创建时间")
    private Datetime create_time_;

    @Column(length = 20)
    @Describe(name = "消费者ip地址")
    private String ip_;

    @Override
    public void onInsertPost(IHandle handle) {
        super.onInsertPost(handle);
        this.setCreate_time_(new Datetime());
    }

    @Override
    public void onUpdatePost(IHandle handle) {
        super.onUpdatePost(handle);
    }

    public Integer getUID_() {
        return UID_;
    }

    public void setUID_(Integer uID_) {
        UID_ = uID_;
    }

    public Integer getQueue_id_() {
        return queue_id_;
    }

    public void setQueue_id_(Integer queue_id_) {
        this.queue_id_ = queue_id_;
    }

    public Integer getAck_() {
        return ack_;
    }

    public void setAck_(Integer ack_) {
        this.ack_ = ack_;
    }

    public String getContent_() {
        return content_;
    }

    public void setContent_(String content_) {
        this.content_ = content_;
    }

    public Datetime getCreate_time_() {
        return create_time_;
    }

    public void setCreate_time_(Datetime create_time_) {
        this.create_time_ = create_time_;
    }

    public String getIp_() {
        return ip_;
    }

    public void setIp_(String ip_) {
        this.ip_ = ip_;
    }

}
