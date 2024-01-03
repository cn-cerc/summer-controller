package cn.cerc.mis.message;

public enum MessageLevel {

    // 通用消息-1个月
    General(30),

    // 重要消息，需要在首页予滚动展示-永久
    Forever(0),

    // 紧急消息，需要弹窗提示-3年
    Grave(3 * 365),

    // 日志类消息，默认为已读-1年
    Logger(365),

    // 后台任务-7天
    Service(7),

    // 打印任务-3天
    Printer(3),

    // 导出消息-1天
    Export(1),

    // 用户普通消息
    User(15);

    private int day;

    MessageLevel(int day) {
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
