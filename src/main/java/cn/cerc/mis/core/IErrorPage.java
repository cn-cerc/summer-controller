package cn.cerc.mis.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.IAppConfig;
import cn.cerc.mis.other.PageNotFoundException;
import cn.cerc.mis.security.SecurityStopException;

public interface IErrorPage {
    Logger log = LoggerFactory.getLogger(IErrorPage.class);

    default void output(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {
        String clientIP = AppClient.getClientIP(request);
        String message = throwable.getMessage();

        if (throwable.getCause() != null) {
            throwable = throwable.getCause();
            message = throwable.getMessage();
        }

        if (throwable instanceof PageNotFoundException)
            log.info("client ip {}, page not found {}", clientIP, message, throwable);
        else if (throwable instanceof UserRequestException)
            log.info("client ip {}, user request error {}", clientIP, message, throwable);
        else if (throwable instanceof SecurityStopException)
            log.warn("client ip {}, security check error {}", clientIP, message, throwable);
        else if (throwable instanceof RuntimeException)
            log.warn("client ip {}, {}", clientIP, message, throwable);
        else
            log.error("client ip {}, {}", clientIP, message, throwable);

        String errorPage = this.getErrorPage(request, response, throwable);
        if (errorPage != null) {
            String path = String.format("/WEB-INF/%s/%s", Application.getBean(IAppConfig.class).getFormsPath(), errorPage);
            try {
                request.getServletContext().getRequestDispatcher(path).forward(request, response);
            } catch (ServletException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    String getErrorPage(HttpServletRequest req, HttpServletResponse resp, Throwable error);

}
