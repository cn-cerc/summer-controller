package cn.cerc.mis.core;

public class ServiceStatus implements IStatus {
//    private int status;
    private int state;
    private String message;

    public ServiceStatus() {
    }

    public ServiceStatus(int state) {
        this.state = state;
        this.message = "";
//        this.status = result ? 200 : 100;
    }

    public ServiceStatus(int state, String message) {
        this.state = state;
        this.message = message;
//        this.status = result ? 200 : 100;
    }

    @Override
    public int getState() {
        return state;
    }

    public ServiceStatus setState(int result) {
        this.state = result;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ServiceStatus setMessage(String message) {
        this.message = message;
        return this;
    }

    public void setResult(boolean result) {
        this.state = result ? 1 : 0;
    }

}
