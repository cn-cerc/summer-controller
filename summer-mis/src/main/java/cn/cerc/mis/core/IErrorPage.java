package cn.cerc.mis.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.mis.other.PageNotFoundException;
import cn.cerc.mis.security.SecurityStopException;

public interface IErrorPage {
    static final Logger _log = LoggerFactory.getLogger(IErrorPage.class);

    default void output(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        Throwable err = e.getCause();
        if (err == null)
            err = e;
        if (e instanceof PageNotFoundException)
            _log.warn("client ip {}, page not found: {}", AppClient.getClientIP(request), e.getMessage());
        else if (e instanceof SecurityStopException)
            _log.warn("client ip {}, {}", AppClient.getClientIP(request), e.getMessage());
        else {
            _log.warn("client ip {}, {}", AppClient.getClientIP(request), err.getMessage(), err);
        }
        String result = this.getErrorPage(request, response, err);
        if (result != null) {
            String url = String.format("/WEB-INF/%s/%s", Application.getConfig().getFormsPath(), result);
            try {
                request.getServletContext().getRequestDispatcher(url).forward(request, response);
            } catch (ServletException | IOException e1) {
                _log.error(e1.getMessage());
                e1.printStackTrace();
            }
        }
    }
    
    String getErrorPage(HttpServletRequest req, HttpServletResponse resp, Throwable error);
}
