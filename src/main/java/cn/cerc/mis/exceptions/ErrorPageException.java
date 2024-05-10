package cn.cerc.mis.exceptions;

import cn.cerc.mis.exception.IKnowall;

public class ErrorPageException extends RuntimeException implements IKnowall {

    private static final long serialVersionUID = 1L;

    private String[] data;

    public ErrorPageException(Class<?> classz, String message, String ip, String url) {
        super(String.format("%s 执行异常：%s", classz.getSimpleName(), message));
        data = new String[] { classz.getSimpleName(), ip, url };
    }

    @Override
    public String[] getData() {
        return data;
    }

    @Override
    public String getGroup() {
        return ErrorPageException.class.getSimpleName();
    }

}
