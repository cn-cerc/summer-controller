package cn.cerc.mis.queue;

public enum NoticeLevelEnum {
    /**
     * 常规等级: 用系统消息通知即可
     */
    Low,
    /**
     * 等级高: 用系统消息与手机简讯同时通知
     */
    High,
    /**
     * 需要签收：在上述通知后，还必须要在系统中点击收到
     */
    Signature;
}
