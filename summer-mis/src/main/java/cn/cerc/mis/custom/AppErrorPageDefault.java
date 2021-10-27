package cn.cerc.mis.custom;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.mis.core.IAppErrorPage;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class AppErrorPageDefault implements IAppErrorPage {

    @Override
    public String getErrorPage(HttpServletRequest req, HttpServletResponse resp, Throwable error) {
        if (error != null) {
            error.printStackTrace();
            String msg = error.toString();
            req.setAttribute("msg", msg.substring(msg.indexOf(":") + 1));
            PrintWriter out;
            try {
                out = resp.getWriter();
                out.println("error: " + msg);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
