package cn.cerc.mis.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.core.ISession;
import cn.cerc.core.Utils;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.BasicHandle;

public class SecurityHandle extends BasicHandle {

    public SecurityHandle(HttpServletRequest request) {
        super();
        ISession session = getSession();

        session.setProperty(ISession.REQUEST, request);
        session.setProperty(Application.SessionId, request.getSession().getId());

        // 获取token
        String token = request.getParameter(ISession.TOKEN);
        if (Utils.isEmpty(token))
            token = request.getParameter("token");

        // 使用token登录，并获取用户资料与授权数据
        session.loadToken(token);
    }

}
