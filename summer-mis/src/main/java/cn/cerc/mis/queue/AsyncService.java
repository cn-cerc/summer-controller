package cn.cerc.mis.queue;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.queue.QueueDB;
import cn.cerc.db.queue.QueueMode;
import cn.cerc.db.queue.QueueQuery;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.client.ServiceSign;
import cn.cerc.mis.core.ServiceQuery;
import cn.cerc.mis.message.MessageLevel;
import cn.cerc.mis.message.MessageProcess;
import cn.cerc.mis.message.MessageRecord;

public class AsyncService extends ServiceQuery {
    public static final String _message_ = "_message_";
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);
    private static final ClassResource res = new ClassResource(AsyncService.class, SummerMIS.ID);

    // 状态列表
    private static List<String> processTiles = new ArrayList<>();

    static {
        processTiles.add(res.getString(1, "中止执行"));
        processTiles.add(res.getString(2, "排队中"));
        processTiles.add(res.getString(3, "正在执行中"));
        processTiles.add(res.getString(4, "执行成功"));
        processTiles.add(res.getString(5, "执行失败"));
    }

    private String corpNo;
    private String userCode;

    // 预约时间，若为空则表示立即执行
    private String timer;
    // 执行进度
    private MessageProcess process = MessageProcess.wait;
    // 处理时间
    private String processTime;
    //
    private MessageLevel messageLevel = MessageLevel.Service;
    //
    private String msgId;

    public AsyncService(IHandle handle) {
        super(handle);
        if (handle != null) {
            this.setCorpNo(handle.getCorpNo());
            this.setUserCode(handle.getUserCode());
        }
    }

    public AsyncService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
    }

    public static String getProcessTitle(int process) {
        return processTiles.get(process);
    }

    public AsyncService read(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonString);
        this.setService(json.get("service").asText());
        if (json.has("dataOut")) {
            this.dataOut().setJson(json.get("dataOut").asText());
        }
        if (json.has("dataIn")) {
            this.dataIn().setJson(json.get("dataIn").asText());
        }
        if (json.has("process")) {
            this.setProcess(MessageProcess.values()[json.get("process").asInt()]);
        }
        if (json.has("timer")) {
            this.setTimer(json.get("timer").asText());
        }
        if (json.has("processTime")) {
            this.setProcessTime(json.get("processTime").asText());
        }
        return this;
    }

    public boolean exec(Object... args) {
        DataRow headIn = dataIn().head();
        if (args.length > 0) {
            if (args.length % 2 != 0) {
                throw new RuntimeException(res.getString(6, "传入的参数数量必须为偶数！"));
            }
            for (int i = 0; i < args.length; i = i + 2) {
                headIn.setValue(args[i].toString(), args[i + 1]);
            }
        }
        headIn.setValue("token", this.getSession().getToken());

        String subject = this.getSubject();
        if ("".equals(subject)) {
            throw new RuntimeException(res.getString(7, "后台任务标题不允许为空！"));
        }
        this.send(); // 发送到队列服务器

        dataOut().head().setValue("_msgId_", msgId);
        if (this.process == MessageProcess.working) {
            // 返回消息的编号插入到阿里云消息队列
            QueueQuery ds = new QueueQuery(this);
            ds.setQueueMode(QueueMode.append);
            ds.add("select * from %s", QueueDB.SUMMER);
            ds.open();
            ds.appendDataSet(this.dataIn(), true);
            ds.head().setValue("_queueId_", msgId);
            ds.head().setValue("_service_", this.serviceId());
            ds.head().setValue("_corpNo_", this.corpNo);
            ds.head().setValue("_userCode_", this.userCode);
            ds.head().setValue("_content_", this.toString());
            ds.save();
        }
        return !"".equals(msgId);
    }

    private void send() {
        String subject = this.getSubject();
        if (subject == null || "".equals(subject)) {
            throw new RuntimeException("subject is null");
        }
        MessageRecord msg = new MessageRecord();
        msg.setCorpNo(this.getCorpNo());
        msg.setUserCode(this.getUserCode());
        msg.setLevel(this.messageLevel);
        msg.setContent(this.toString());
        msg.setSubject(subject);
        msg.setProcess(this.process);
        log.debug(this.getCorpNo() + ":" + this.getUserCode() + ":" + this);
        this.msgId = msg.send(this);
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode content = mapper.createObjectNode();

        content.put("service", this.serviceId());
        if (this.dataIn() != null) {
            content.put("dataIn", dataIn().json());
        }
        if (this.dataOut() != null) {
            content.put("dataOut", dataOut().json());
        }
        content.put("timer", this.timer);
        content.put("process", this.process.ordinal());
        if (this.processTime != null) {
            content.put("processTime", this.processTime);
        }
        return content.toString();
    }

    public String getService() {
        return serviceId();
    }

    @Override
    public AsyncService setService(ServiceSign service) {
        super.setService(service);
        return this;
    }

    @Deprecated
    public AsyncService setService(String service) {
        super.setService(new ServiceSign(service));
        return this;
    }

    public MessageProcess getProcess() {
        return process;
    }

    public void setProcess(MessageProcess process) {
        this.process = process;
    }

    public String getTimer() {
        return timer;
    }

    public void setTimer(String timer) {
        this.timer = timer;
    }

    public String getProcessTime() {
        return processTime;
    }

    public void setProcessTime(String processTime) {
        this.processTime = processTime;
    }

    @Override
    public String getCorpNo() {
        return corpNo;
    }

    public void setCorpNo(String corpNo) {
        this.corpNo = corpNo;
    }

    @Override
    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String message() {
        if (super.dataOut() == null)
            return null;
        if (!super.dataOut().head().exists(_message_))
            return null;
        return super.dataOut().head().getString(_message_);
    }

    public MessageLevel getMessageLevel() {
        return messageLevel;
    }

    public void setMessageLevel(MessageLevel messageLevel) {
        this.messageLevel = messageLevel;
    }

    public String getSubject() {
        return dataIn().head().getString("_subject_");
    }

    public void setSubject(String subject) {
        dataIn().head().setValue("_subject_", subject);
    }

    public void setSubject(String format, Object... args) {
        dataIn().head().setValue("_subject_", String.format(format, args));
    }

    public String getMsgId() {
        return msgId;
    }

    @Deprecated
    public String getMessage() {
        return this.message();
    }
}
