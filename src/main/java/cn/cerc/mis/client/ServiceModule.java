package cn.cerc.mis.client;

/**
 * 制造业的三种模式——OEM、ODM和OBM
 */
public enum ServiceModule {

    CSP("云软件平台企业"),
    OBM("自有品牌型企业"),
    ODM("重度定制型企业"),
    OEM("轻度定制型企业"),
    Material("钢铁产业类企业"),
    FPL("物流产业类企业"),
    NPL("网络货运企业"),
    CSM("云仓平台企业");

    private final String value;

    ServiceModule(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
