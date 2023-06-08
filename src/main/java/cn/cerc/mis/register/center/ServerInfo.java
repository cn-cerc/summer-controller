package cn.cerc.mis.register.center;

import java.io.Serializable;

public class ServerInfo implements Serializable {

    private static final long serialVersionUID = -8863308062165311147L;

    public ServerInfo() {
    }

    public ServerInfo(String lanIp, String lanPort, String original, String wanIp, String wanPort) {
        super();
        this.lanIp = lanIp;
        this.lanPort = lanPort;
        this.original = original;
        this.wanIp = wanIp;
        this.wanPort = wanPort;
    }

    private String lanIp;
    private String lanPort;
    private String original;
    private String wanIp;
    private String wanPort;

    public String getLanIp() {
        return lanIp;
    }

    public void setLanIp(String lanIp) {
        this.lanIp = lanIp;
    }

    public String getLanPort() {
        return lanPort;
    }

    public void setLanPort(String lanPort) {
        this.lanPort = lanPort;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getWanIp() {
        return wanIp;
    }

    public void setWanIp(String wanIp) {
        this.wanIp = wanIp;
    }

    public String getWanPort() {
        return wanPort;
    }

    public void setWanPort(String wanPort) {
        this.wanPort = wanPort;
    }

    @Override
    public String toString() {
        return "ServerInfo [lanIp=" + lanIp + ", lanPort=" + lanPort + ", original=" + original + ", wanIp=" + wanIp
                + ", wanPort=" + wanPort + "]";
    }

}
