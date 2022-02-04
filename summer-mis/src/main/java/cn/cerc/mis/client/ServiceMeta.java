package cn.cerc.mis.client;

public final class ServiceMeta {
    private String id;
    private String site;

    public ServiceMeta(String id) {
        super();
        this.id = id;
    }

    public ServiceMeta(String id, String site) {
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
