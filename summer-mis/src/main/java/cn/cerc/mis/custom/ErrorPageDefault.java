package cn.cerc.mis.custom;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.mis.core.AppClient;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IAppErrorPage;
import cn.cerc.mis.core.IErrorPage;
import cn.cerc.mis.other.PageNotFoundException;
import cn.cerc.mis.security.SecurityStopException;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class ErrorPageDefault implements IErrorPage {
    private static final Logger log = LoggerFactory.getLogger(ErrorPageDefault.class);
    
    @Override
    public void output( HttpServletRequest request, HttpServletResponse response, Throwable e) {
        if (e instanceof PageNotFoundException)
            log.warn("client ip {}, page not found: {}", AppClient.getClientIP(request), e.getMessage());
        else if (e instanceof SecurityStopException)
            log.warn("client ip {}, {}", AppClient.getClientIP(request), e.getMessage());
        else
            log.warn("client ip {}, {}", AppClient.getClientIP(request), e.getMessage(), e);
        Throwable err = e.getCause();
        if (err == null) {
            err = e;
        }
        IAppErrorPage errorPage = Application.getBean(IAppErrorPage.class);
        if (errorPage != null) {
            String result = errorPage.getErrorPage(request, response, err);
            if (result != null) {
                String url = String.format("/WEB-INF/%s/%s", Application.getConfig().getFormsPath(), result);
                try {
                    request.getServletContext().getRequestDispatcher(url).forward(request, response);
                } catch (ServletException | IOException e1) {
                    log.error(e1.getMessage());
                    e1.printStackTrace();
                }
            }
        } else {
            log.warn("not define bean: errorPage");
            log.error(err.getMessage());
            err.printStackTrace();
        }
    }

}
