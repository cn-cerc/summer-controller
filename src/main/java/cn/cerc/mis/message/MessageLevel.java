package cn.cerc.mis.message;

public enum MessageLevel {

    // 通用消息-3个月
    General(3 * 33),

    // 重要消息，需要在首页予滚动展示-永久
    Great(0),

    // 紧急消息，需要弹窗提示-3年
    Grave(3 * 12 * 33),

    // 日志类消息，默认为已读-1年
    Logger(12 * 33),

    // 后台任务-33天
    Service(33),

    // 打印任务-3天
    Printer(3),

    // 导出消息-2天
    Export(2),

    // 用户普通消息
    User(30);

    private int day;

    private MessageLevel(int day) {
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public static MessageLevel getItem(int val) {
        MessageLevel value = null;
        for (MessageLevel item : values()) {
            if (item.ordinal() == val) {
                value = item;
                break;
            }
        }
        if (value == null)
            throw new RuntimeException(String.format("不支持的消息类别 %s", val));
        return value;
    }

}
