package cn.cerc.mis.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.IAppConfig;
import cn.cerc.db.core.Utils;
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

        String url = this.getUrl(request);
        if (throwable instanceof PageNotFoundException)
            log.info("ip {}, url {}, 页面异常 {}", clientIP, url, message, throwable);
        if (throwable instanceof DataException)
            log.info("ip {}, url {}, 数据检查异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof UserRequestException)
            log.info("ip {}, url {}, 请求异常 {}", clientIP, url, message, throwable);
        if (throwable instanceof NoSuchMethodException)
            log.info("ip {}, url {}, 方法异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof IllegalArgumentException)
            log.info("ip {}, url {}, 参数异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof UnsupportedOperationException)
            log.info("ip {}, url {}, 异常操作 {}", clientIP, url, message, throwable);
        else if (throwable instanceof SecurityStopException)
            // FIXME 暂时降低日志等级，等待后续能够捕捉权限不足超链接来源时再将日志等级恢复成警告
            log.info("ip {}, url {}, 权限检查异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof IOException)
            log.error("ip {}, url {}, io异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof ServiceExecuteException)
            log.error("ip {}, url {}, 服务执行异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof ServletException)
            log.error("ip {}, url {}, servlet异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof ReflectiveOperationException)
            log.error("ip {}, url {}, 反射异常 {}", clientIP, url, message, throwable);
        else if (throwable instanceof RuntimeException)
            log.error("ip {}, url {}, 运行异常 {}", clientIP, url, message, throwable);
        else
            log.warn("ip {}, url {}, 其他异常 {}", clientIP, url, message, throwable);

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

    private String getUrl(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        String site = request.getRequestURL().toString();
        builder.append(site);
        Map<String, String> items = this.convert(request.getParameterMap());
        int i = 0;
        for (String key : items.keySet()) {
            i++;
            builder.append(i == 1 ? "?" : "&");
            builder.append(key);
            builder.append("=");
            String value = items.get(key);
            if (value != null) {
                builder.append(Utils.encode(value, StandardCharsets.UTF_8.name()));
            }
        }
        return builder.toString();
    }

    /**
     * 解析 request 的请求参数
     */
    private Map<String, String> convert(Map<String, String[]> params) {
        Map<String, String> items = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length; i++) {
                builder.append(value[i]);
                if (i < value.length - 1) {
                    builder.append(",");
                }
            }
            items.put(key, builder.toString());
        });
        return items;
    }

    String getErrorPage(HttpServletRequest req, HttpServletResponse resp, Throwable error);

}
