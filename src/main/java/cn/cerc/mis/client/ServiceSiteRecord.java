package cn.cerc.mis.client;

public record ServiceSiteRecord(boolean isLan, String industry, String host) {

    /**
     * 
     * @return 根据内网还是外网标识，返回相应的网址
     */
    public String website() {
        // 192.168.1.20:9101/services/
        if (isLan())
            return String.format("%s/services/", host());
        else
            // www.4plc.cn/services-csp/
            return String.format("%s/services-%s/", host(), industry());
    }

}
