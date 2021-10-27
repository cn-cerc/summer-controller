package cn.cerc.mis.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IErrorPage {

    void output(HttpServletRequest request, HttpServletResponse response, Throwable e);

}
