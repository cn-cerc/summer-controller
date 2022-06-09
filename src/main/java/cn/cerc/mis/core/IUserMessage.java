package cn.cerc.mis.core;

import java.util.List;

import cn.cerc.db.core.DataRow;
import cn.cerc.mis.message.MessageLevel;
import cn.cerc.mis.message.MessageProcess;

/**
 * 数据库消息队列
 * 
 * @author ZhangGong
 *
 */
public interface IUserMessage {
    /**
     * @return 取出所有的等待处理的消息列表
     */
    List<String> getWaitList();

    /**
     * 增加新的消息，并返回消息编号（msgID）
     * 
     * @param corpNo   帐套代码
     * @param userCode 用户代码
     * @param level    消息等级
     * @param subject  消息标题
     * @param content  消息内容
     * @param process  处理进度
     * @param UIClass  消息类别
     * @return 返回消息编号（msgID）
     */
    String appendRecord(String corpNo, String userCode, MessageLevel level, String subject, String content,
            MessageProcess process,String UIClass);

    /**
     * 读取待处理的任务：队列服务
     * 
     * @param msgId 消息ID
     * @return 消息内容
     */
    DataRow readAsyncService(String msgId);

    /**
     * 更新任务处理进度：队列服务
     * 
     * @param msgId   消息id
     * @param content 消息内容
     * @param process 处理进度
     * @return 更新结果
     */
    boolean updateAsyncService(String msgId, String content, MessageProcess process);
}
