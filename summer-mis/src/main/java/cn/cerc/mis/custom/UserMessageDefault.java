package cn.cerc.mis.custom;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cn.cerc.core.Datetime;
import cn.cerc.core.ISession;
import cn.cerc.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.mysql.MysqlQuery;
import cn.cerc.db.redis.Redis;
import cn.cerc.mis.core.ISystemTable;
import cn.cerc.mis.core.IUserMessage;
import cn.cerc.mis.core.SystemBufferType;
import cn.cerc.mis.message.MessageLevel;
import cn.cerc.mis.message.MessageProcess;
import cn.cerc.mis.message.MessageRecord;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserMessageDefault implements IHandle, IUserMessage {

    @Autowired
    private ISystemTable systemTable;
    private ISession session;

    @Override
    public List<String> getWaitList() {
        List<String> result = new ArrayList<>();
        MysqlQuery ds = new MysqlQuery(this);
        ds.setMaximum(5);
        ds.add("select ms.UID_ from %s ms", systemTable.getUserMessages());
        ds.add("where ms.Level_=%s", MessageLevel.Service.ordinal());
        ds.add("and ms.Process_=%s", MessageProcess.wait.ordinal());
        ds.open();
        while (ds.fetch()) {
            result.add(ds.getString("UID_"));
        }
        return result;
    }

    @Override
    public String appendRecord(String corpNo, String userCode, MessageLevel level, String subject, String content,
            MessageProcess process) {
        // 若为异步任务消息请求
        if (level == MessageLevel.Service) {
            // 若已存在同一公司别同一种回算请求在排队或者执行中，则不重复插入回算请求
            MysqlQuery ds2 = new MysqlQuery(this);
            ds2.setMaximum(1);
            ds2.add("select UID_ from %s ", systemTable.getUserMessages());
            ds2.add("where CorpNo_='%s' ", corpNo);
            ds2.add("and Subject_='%s' ", subject);
            ds2.add("and Level_=4 and (Process_ = 1 or Process_=2)");
            ds2.open();
            if (ds2.size() > 0) {
                // 返回消息的编号
                return ds2.getString("UID_");
            }
        }

        MysqlQuery cdsMsg = new MysqlQuery(this);
        cdsMsg.add("select * from %s", systemTable.getUserMessages());
        cdsMsg.setMaximum(0);
        cdsMsg.open();

        // 保存到数据库
        cdsMsg.append();
        cdsMsg.setField("CorpNo_", corpNo);
        cdsMsg.setField("UserCode_", userCode);
        cdsMsg.setField("Level_", level.ordinal());
        cdsMsg.setField("Subject_", subject);
        if (content.length() > 0) {
            cdsMsg.setField("Content_", content);
        }
        cdsMsg.setField("AppUser_", session.getUserCode());
        cdsMsg.setField("AppDate_", new Datetime());
        // 日志类消息默认为已读
        cdsMsg.setField("Status_", level == MessageLevel.Logger ? 1 : 0);
        cdsMsg.setField("Process_", process == null ? 0 : process.ordinal());
        cdsMsg.setField("Final_", false);
        cdsMsg.post();

        // 清除缓存
        String buffKey = String.format("%d.%s.%s.%s", SystemBufferType.getObject.ordinal(), MessageRecord.class, corpNo,
                userCode);
        Redis.delete(buffKey);

        // 返回消息的编号
        return cdsMsg.getString("UID_");
    }

    @Override
    public DataRow readAsyncService(String msgId) {
        MysqlQuery ds = new MysqlQuery(this);
        ds.add("select * from %s", systemTable.getUserMessages());
        ds.add("where Level_=%s", MessageLevel.Service.ordinal());
        ds.add("and Process_=%s", MessageProcess.wait.ordinal());
        ds.add("and UID_='%s'", msgId);
        ds.open();
        // 此任务可能被其它主机抢占
        if (ds.eof()) {
            return null;
        }

        DataRow result = new DataRow();
        result.setField("corpNo", ds.getString("CorpNo_"));
        result.setField("userCode", ds.getString("UserCode_"));
        result.setField("subject", ds.getString("Subject_"));
        result.setField("content", ds.getString("Content_"));
        return result;
    }

    @Override
    public boolean updateAsyncService(String msgId, String content, MessageProcess process) {
        MysqlQuery cdsMsg = new MysqlQuery(this);
        cdsMsg.add("select * from %s", systemTable.getUserMessages());
        cdsMsg.add("where UID_='%s'", msgId);
        cdsMsg.open();
        if (cdsMsg.eof()) {
            return false;
        }

        cdsMsg.edit();
        cdsMsg.setField("Content_", content);
        cdsMsg.setField("Process_", process.ordinal());
        if (process == MessageProcess.ok) {
            cdsMsg.setField("Status_", 1);
        }
        cdsMsg.post();

        if (process == MessageProcess.ok) {
            // 清除缓存
            String buffKey = String.format("%d.%s.%s.%s", SystemBufferType.getObject.ordinal(), MessageRecord.class,
                    cdsMsg.getString("CorpNo_"), cdsMsg.getString("UserCode_"));
            Redis.delete(buffKey);
        }
        return true;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

}
