package cn.cerc.mis.other;

import java.io.Serial;

public class PageNotFoundException extends Exception {
    @Serial
    private static final long serialVersionUID = -5429805661412370832L;

    public PageNotFoundException(String message) {
        super(message);
    }

}
