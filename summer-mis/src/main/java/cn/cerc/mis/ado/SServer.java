package cn.cerc.mis.ado;

public class SServer {
    private static final String LOCALHOST = "http://127.0.0.1";
    private String host;
    private String token;

    public SServer() {
        super();
        host = LOCALHOST;
    }

    public final String host() {
        return host;
    }

    public final SServer setHost(String host) {
        this.host = host;
        return this;
    }

    public final String token() {
        return token;
    }

    public final SServer setToken(String token) {
        this.token = token;
        return this;
    }
}
