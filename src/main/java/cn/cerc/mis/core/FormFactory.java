package cn.cerc.mis.core;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.cache.ISessionCache;
import cn.cerc.mis.other.PageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class FormFactory implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(FormFactory.class);
    // FIXME: 此处资源文件引用特殊，需要连动所有项目一起才能修改
    private static final ClassResource res = new ClassResource(FormFactory.class, SummerMIS.ID);
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        Application.setContext(applicationContext);
    }

    public String getView(IHandle handle, HttpServletRequest req, HttpServletResponse resp, String formId,
                          String funcCode, String... pathVariables) {
        // 设置登录开关
        req.setAttribute("logon", false);

        // 建立数据库资源
        try {
            req.setAttribute("myappHandle", handle);
            ISession session = handle.getSession();
            session.setProperty(Application.SessionId, req.getSession().getId());
            session.setProperty(ISession.REQUEST, req);

            IForm form = null;
            String beanId = formId;
            if (!Utils.isEmpty(beanId) && !"service".equals(beanId)) {
                if (!context.containsBean(beanId)) {
                    if (!beanId.substring(0, 2).toUpperCase().equals(beanId.substring(0, 2)))
                        beanId = beanId.substring(0, 1).toLowerCase() + beanId.substring(1);
                }
                if (context.containsBean(beanId))
                    form = context.getBean(beanId, IForm.class);
            }
            if (form == null)
                throw new PageNotFoundException(req.getServletPath());

            form.setSession(session);

            String token = (String) req.getSession().getAttribute(ISession.TOKEN);
            session.loadToken(token);

            // 取出自定义session中用户设置的语言类型，并写入到request
            req.setAttribute(ISession.LANGUAGE_ID, session.getProperty(ISession.LANGUAGE_ID));
            req.getSession().setAttribute(ISession.LANGUAGE_ID, session.getProperty(ISession.LANGUAGE_ID));

            req.setAttribute("_showMenu_", !AppClient.ee.equals(form.getClient().getDevice()));
            form.setId(formId);
            // 传递路径变量
            form.setPathVariables(pathVariables);
//
//            // 匿名访问
            if (form._isAllowGuest())
                return form._call(funcCode);

            // 是否登录
            if (!session.logon()) {
                // 登录验证
                IAppLogin appLogin = Application.getBean(form, IAppLogin.class);
                String loginView = appLogin.getLoginView(form);
                // 刷新session缓存
                Map<String, ISessionCache> items = Application.getContext().getBeansOfType(ISessionCache.class);
                items.forEach((k, v) -> v.clearCache());
                if ("".equals(loginView)) {
                    return null;
                }
                if (loginView != null) {
                    return loginView;
                }
            }

            // 设备检查
            if (form.isSecurityDevice()) {
                return form._call(funcCode);
            }

            ISecurityDeviceCheck deviceCheck = Application.getBean(form, ISecurityDeviceCheck.class);
            switch (deviceCheck.pass(form)) {
                case PASS:
                    log.debug("{}.{}", formId, funcCode);
                    return form._call(funcCode);
                case CHECK:
                    return "redirect:" + Application.getConfig().getVerifyDevicePage();
                case LOGIN:
                    // 登录验证
                    IAppLogin appLogin = Application.getBean(form, IAppLogin.class);
                    String loginView = appLogin.getLoginView(form);
                    if ("".equals(loginView)) {
                        return null;
                    }
                    if (loginView != null) {
                        return loginView;
                    }
                default:
                    resp.setContentType("text/html;charset=UTF-8");
                    IErrorPage error = context.getBean(IErrorPage.class);
                    error.output(req, resp, new RuntimeException(res.getString(2, "对不起，当前设备被禁止使用！")));
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            IErrorPage error = context.getBean(IErrorPage.class);
            error.output(req, resp, e);
            return null;
        }
    }

    public void outputView(HttpServletRequest request, HttpServletResponse response, String url)
            throws IOException, ServletException {
        if (url == null)
            return;

        if (url.startsWith("redirect:")) {
            String redirect = url.substring(9);
            redirect = response.encodeRedirectURL(redirect);
            response.sendRedirect(redirect);
            return;
        }

        // 输出jsp文件
        String jspFile = String.format("/WEB-INF/%s/%s", Application.getConfig().getFormsPath(), url);
        request.getServletContext().getRequestDispatcher(jspFile).forward(request, response);
    }

}
