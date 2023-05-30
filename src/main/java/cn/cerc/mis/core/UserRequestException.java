package cn.cerc.mis.core;

import java.io.Serial;
import java.io.Serializable;

/**
 * 处理用户请求的异常
 */
public class UserRequestException extends Exception implements Serializable {

    @Serial
    private static final long serialVersionUID = -687938560794136116L;

    public UserRequestException(String message) {
        super(message);
    }

}
