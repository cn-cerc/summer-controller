package cn.cerc.mis.ado;

/**
 * 无效的实体异常类
 */
public class InvalidEntityException extends RuntimeException {

    private static final long serialVersionUID = -4810945733324586294L;

    public InvalidEntityException(String message) {
        super(message);
    }

}
