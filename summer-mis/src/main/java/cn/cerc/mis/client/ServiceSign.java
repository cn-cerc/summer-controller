package cn.cerc.mis.client;

public final class ServiceSign {
    private String id;
    private String site;

    public ServiceSign(String id) {
        super();
        this.id = id;
    }

    public ServiceSign(String id, String site) {
        super();
        this.id = id;
        this.site = site;
    }

    public String id() {
        return id;
    }

    public String site() {
        return this.site;
    }

}
