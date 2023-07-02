package cn.cerc.mis.client;

public record ServiceSiteRecord(boolean isLan, String industry, String host) {
    /**
     * 
     * @return 根据内网还是外网标识，返回相应的网址
     */
    public String website() {
        if (isLan())
            return String.format("%s/services/%s", host());
        else
            return String.format("%s/services-%s/%s", host(), industry());
    }
}
