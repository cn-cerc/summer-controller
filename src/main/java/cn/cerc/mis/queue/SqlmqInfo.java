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
@EntityKey(fields = { "status_", "order_" })
@Table(name = SqlmqInfo.TABLE, indexes = { @Index(name = "PRIMARY", columnList = "UID_", unique = true),
        @Index(name = "ix_product_", columnList = "product_, industry_, status_, show_time_, service_"),
        @Index(name = "ix_queue_status_time", columnList = "queue_, status_, show_time_"),
        @Index(name = "ix_status_order", columnList = "status_, order_"),
        @Index(name = "ix_status_show_time", columnList = "status_, show_time_, service_, queue_") })
@SqlServer(type = SqlServerType.Mysql)
public class SqlmqInfo extends CustomEntity {
    public static final String TABLE = "s_sqlmq_info";

    @Id
    @GeneratedValue
    @Column(length = 11, nullable = false)
    @Describe(name = "主键ID")
    private Integer UID_;

    @Column(length = 100, nullable = false)
    @Describe(name = "队列名称")
    private String queue_;

    @Column(length = 30)
    @Describe(name = "业务标识")
    private String order_;

    @Column(nullable = false)
    @Describe(name = "消息内容 dataSet")
    private String message_;

    @Column(nullable = false, columnDefinition = "datetime")
    @Describe(name = "可消费时间")
    private Datetime show_time_;

    @Column(length = 11, nullable = false)
    @Describe(name = "消费次数")
    private Integer consume_times_;

    @Column(length = 11, nullable = false)
    @Describe(name = "消息状态（0-待消费 1-消费中 2-已完成 3-失败或未知4已作废）")
    private Integer status_;

    @Column(length = 11, nullable = false)
    @Describe(name = "当前版本")
    private Integer version_;

    @Column(length = 11)
    @Describe(name = "延迟时间(默认60s)")
    private Integer delayTime_;

    @Column(length = 11)
    @Describe(name = "队列类型(默认Sqlmq)")
    private Integer service_;

    @Column(length = 10)
    @Describe(name = "产品别")
    private String product_;

    @Column(length = 10)
    @Describe(name = "产业别")
    private String industry_;

    @Column(length = 50)
    @Describe(name = "执行队列")
    private String queue_class_;

    @Column(length = 11, nullable = false)
    @Describe(name = "创建人员")
    private String create_user_;

    @Column(nullable = false, columnDefinition = "datetime")
    @Describe(name = "创建时间")
    private Datetime create_time_;

    @Column(nullable = false, columnDefinition = "datetime")
    @Describe(name = "更新时间")
    private Datetime update_time_;

    @Override
    public void onInsertPost(IHandle handle) {
        super.onInsertPost(handle);
        this.setCreate_time_(new Datetime());
        this.setUpdate_time_(new Datetime());
        this.setCreate_user_(handle.getUserCode());
    }

    @Override
    public void onUpdatePost(IHandle handle) {
        super.onUpdatePost(handle);
        this.setUpdate_time_(new Datetime());
    }

    public Integer getUID_() {
        return UID_;
    }

    public void setUID_(Integer uID_) {
        UID_ = uID_;
    }

    public String getQueue_() {
        return queue_;
    }

    public void setQueue_(String queue_) {
        this.queue_ = queue_;
    }

    public String getOrder_() {
        return order_;
    }

    public void setOrder_(String order_) {
        this.order_ = order_;
    }

    public String getMessage_() {
        return message_;
    }

    public void setMessage_(String message_) {
        this.message_ = message_;
    }

    public Datetime getShow_time_() {
        return show_time_;
    }

    public void setShow_time_(Datetime show_time_) {
        this.show_time_ = show_time_;
    }

    public Integer getConsume_times_() {
        return consume_times_;
    }

    public void setConsume_times_(Integer consume_times_) {
        this.consume_times_ = consume_times_;
    }

    public Integer getStatus_() {
        return status_;
    }

    public void setStatus_(Integer status_) {
        this.status_ = status_;
    }

    public Integer getVersion_() {
        return version_;
    }

    public void setVersion_(Integer version_) {
        this.version_ = version_;
    }

    public Integer getDelayTime_() {
        return delayTime_;
    }

    public void setDelayTime_(Integer delayTime_) {
        this.delayTime_ = delayTime_;
    }

    public Integer getService_() {
        return service_;
    }

    public void setService_(Integer service_) {
        this.service_ = service_;
    }

    public String getProduct_() {
        return product_;
    }

    public void setProduct_(String product_) {
        this.product_ = product_;
    }

    public String getIndustry_() {
        return industry_;
    }

    public void setIndustry_(String industry_) {
        this.industry_ = industry_;
    }

    public String getQueue_class_() {
        return queue_class_;
    }

    public void setQueue_class_(String queue_class_) {
        this.queue_class_ = queue_class_;
    }

    public String getCreate_user_() {
        return create_user_;
    }

    public void setCreate_user_(String create_user_) {
        this.create_user_ = create_user_;
    }

    public Datetime getCreate_time_() {
        return create_time_;
    }

    public void setCreate_time_(Datetime create_time_) {
        this.create_time_ = create_time_;
    }

    public Datetime getUpdate_time_() {
        return update_time_;
    }

    public void setUpdate_time_(Datetime update_time_) {
        this.update_time_ = update_time_;
    }

}
