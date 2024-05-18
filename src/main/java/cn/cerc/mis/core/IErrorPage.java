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
import cn.cerc.db.log.KnowallLog;
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

        String method = request.getMethod();
        String url = this.getUrl(request);
        if (throwable instanceof PageNotFoundException)
            log.info("找不到页面 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof UserRequestException)
            log.info("请求异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof NoSuchMethodException)
            log.info("找不到方法 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof IllegalArgumentException)
            log.info("参数数量异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof UnsupportedOperationException)
            log.info("异常操作 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof DataException)
            log.warn("数据校验失败 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof SecurityStopException)
            log.warn("用户权限不足 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof IOException)
            log.error("IO异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof ServiceExecuteException)
            log.error("服务执行异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof ServletException)
            log.error("servlet异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof ReflectiveOperationException)
            log.error("反射异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        else if (throwable instanceof NullPointerException)
            log.error("空指针异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
       else if (throwable instanceof RuntimeException) {
            log.error("运行异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        } else {
            log.error("未知异常 {}", message, KnowallLog.of(throwable).add(clientIP).add(method).add(url));
        }

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
