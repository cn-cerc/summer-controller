package cn.cerc.mis.core;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

@Deprecated
public interface IFormFilter {
    boolean doFilter(HttpServletResponse resp, String formId, String funcCode) throws IOException;
}
