package cn.cerc.mis.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import cn.cerc.core.ISession;
import cn.cerc.mis.security.Permission;
import cn.cerc.mis.security.SecurityPolice;
import cn.cerc.mis.security.SecurityStopException;
import cn.cerc.mis.security.Webform;

//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractForm implements IForm, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(AbstractForm.class);
//    private static final ClassResource res = new ClassResource(AbstractForm.class, SummerMIS.ID);
//    private static final ClassConfig config = new ClassConfig(AbstractForm.class, SummerMIS.ID);

    private String id;
    @Autowired
    private AppClient client;

    @Autowired
    private ISession session;

    private Map<String, String> params = new HashMap<>();
    private String name;
    private String parent;
    private String permission;
    private String module;
    private String[] pathVariables;
    private String beanName;

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public HttpServletRequest getRequest() {
        return this.getSession().getRequest();
    }

    @Override
    public HttpServletResponse getResponse() {
        return this.getSession().getResponse();
    }

    @Override
    public AppClient getClient() {
        return this.client;
    }

    @Override
    public Object getProperty(String key) {
        if ("request".equals(key)) {
            return this.getRequest();
        }
        if ("session".equals(key)) {
            return this.getRequest().getSession();
        }

        return this.getSession().getProperty(key);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public final void setCaption(String name) {
        setName(name);
    }

    @Override
    public void setParam(String key, String value) {
        params.put(key, value);
    }

    @Override
    public String getParam(String key, String def) {
        return params.getOrDefault(key, def);
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    // 执行指定函数，并返回jsp文件名，若自行处理输出则直接返回null
    @Override
    public String call(String funcCode) throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ServletException, IOException {
        HttpServletResponse response = getResponse();
        if ("excel".equals(funcCode)) {
            response.setContentType("application/vnd.ms-excel; charset=UTF-8");
            response.addHeader("Content-Disposition", "attachment; filename=excel.csv");
        } else {
            response.setContentType("text/html;charset=UTF-8");
        }

        HttpServletRequest request = this.getRequest();
        String CLIENTVER = request.getParameter("CLIENTVER");
        if (CLIENTVER != null)
            request.getSession().setAttribute("CLIENTVER", CLIENTVER);

        Object result;
        Method method = null;
        long startTime = System.currentTimeMillis();
        try {
            // 支持路径参数调用，最多3个字符串参数
            switch (this.pathVariables.length) {
            case 1: {
                if (this.getClient().isPhone()) {
                    try {
                        method = this.getClass().getMethod(funcCode + "_phone", String.class);
                    } catch (NoSuchMethodException e) {
                        method = this.getClass().getMethod(funcCode, String.class);
                    }
                } else {
                    method = this.getClass().getMethod(funcCode, String.class);
                }
                if (!SecurityPolice.check(this, method, this)) {
                    throw new SecurityStopException(method, this);
                }
                result = method.invoke(this, this.pathVariables[0]);
                break;
            }
            case 2: {
                if (this.getClient().isPhone()) {
                    try {
                        method = this.getClass().getMethod(funcCode + "_phone", String.class, String.class);
                    } catch (NoSuchMethodException e) {
                        method = this.getClass().getMethod(funcCode, String.class, String.class);
                    }
                } else {
                    method = this.getClass().getMethod(funcCode, String.class, String.class);
                }
                if (!SecurityPolice.check(this, method, this)) {
                    throw new SecurityStopException(method, this);
                }
                result = method.invoke(this, this.pathVariables[0], this.pathVariables[1]);
                break;
            }
            case 3: {
                if (this.getClient().isPhone()) {
                    try {
                        method = this.getClass().getMethod(funcCode + "_phone", String.class, String.class,
                                String.class);
                    } catch (NoSuchMethodException e) {
                        method = this.getClass().getMethod(funcCode, String.class, String.class, String.class);
                    }
                } else {
                    method = this.getClass().getMethod(funcCode, String.class, String.class, String.class);
                }
                if (!SecurityPolice.check(this, method, this)) {
                    throw new SecurityStopException(method, this);
                }
                result = method.invoke(this, this.pathVariables[0], this.pathVariables[1], this.pathVariables[2]);
                break;
            }
            default: {
                if (this.getClient().isPhone()) {
                    try {
                        method = this.getClass().getMethod(funcCode + "_phone");
                    } catch (NoSuchMethodException e) {
                        method = this.getClass().getMethod(funcCode);
                    }
                } else {
                    method = this.getClass().getMethod(funcCode);
                }
                if (!SecurityPolice.check(this, method, this)) {
                    throw new SecurityStopException(method, this);
                }
                result = method.invoke(this);
            }
            }

            if (result == null)
                return null;

            if (result instanceof IPage) {
                IPage output = (IPage) result;
                return output.execute();
            } else {
                log.warn(String.format("%s pageOutput is not IView: %s", funcCode, result));
                return (String) result;
            }
        } catch (PageException e) {
            this.setParam("message", e.getMessage());
            return e.getViewFile();
        } finally {
            if (method != null)
                checkTimeout(this, funcCode, startTime);
        }
    }

    private void checkTimeout(IForm form, String funcCode, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        if (totalTime > 3000) {
            String[] tmp = form.getClass().getName().split("\\.");
            String pageCode = tmp[tmp.length - 1] + "." + funcCode;
            String dataIn = new Gson().toJson(form.getRequest().getParameterMap());
            if (dataIn.length() > 200) {
                dataIn = dataIn.substring(0, 200);
            }
            log.info("{}, tickCount: {}, dataIn: {}", pageCode, totalTime, dataIn);
        }
    }

    @Override
    public void setPathVariables(String[] pathVariables) {
        this.pathVariables = pathVariables;
    }

    public String[] getPathVariables() {
        return this.pathVariables;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public ISession getSession() {
        return this.session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Webform obj = this.getClass().getAnnotation(Webform.class);
        if (obj != null) {
            this.name = obj.name();
            this.module = obj.module();
            this.parent = obj.parent();
        }
        Permission ps = this.getClass().getAnnotation(Permission.class);
        if (ps != null) {
            this.permission = ps.value();
        }
    }
}
