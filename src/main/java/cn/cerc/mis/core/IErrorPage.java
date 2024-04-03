package cn.cerc.mis.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.IAppConfig;
import cn.cerc.mis.client.ServiceExecuteException;
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
            log.info("用户地址 {}, 页面异常 {}", clientIP, message, throwable);
        else if (throwable instanceof UserRequestException)
            log.info("用户地址 {}, 请求异常 {}", clientIP, message, throwable);
        else if (throwable instanceof SecurityStopException)
            log.warn("用户地址 {}, 权限校验异常 {}", clientIP, message, throwable);
        else if (throwable instanceof IOException)
            log.error("用户地址 {}, io异常 {}", clientIP, message, throwable);
        else if (throwable instanceof ServiceExecuteException)
            log.error("用户地址 {}, 服务执行异常 {}", clientIP, message, throwable);
        else if (throwable instanceof ServletException)
            log.error("用户地址 {}, servlet异常 {}", clientIP, message, throwable);
        else if (throwable instanceof IllegalArgumentException)
            log.error("用户地址 {}, 参数异常 {}", clientIP, message, throwable);
        else if (throwable instanceof ReflectiveOperationException)
            log.error("用户地址 {}, 反射异常 {}", clientIP, message, throwable);
        else if (throwable instanceof RuntimeException)
            log.error("用户地址 {}, 运行异常 {}", clientIP, message, throwable);
        else
            log.warn("用户地址 {}, 其他异常 {}", clientIP, message, throwable);

        String errorPage = this.getErrorPage(request, response, throwable);
        if (errorPage != null) {
            String path = String.format("/WEB-INF/%s/%s", Application.getBean(IAppConfig.class).getFormsPath(),
                    errorPage);
            try {
                request.getServletContext().getRequestDispatcher(path).forward(request, response);
            } catch (ServletException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    String getErrorPage(HttpServletRequest req, HttpServletResponse resp, Throwable error);

}
