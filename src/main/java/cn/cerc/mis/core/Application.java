package cn.cerc.mis.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.IAppConfig;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.db.zk.ZkNode;
import cn.cerc.mis.SummerMIS;

@Component
public class Application implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final ClassConfig config = new ClassConfig(Application.class, SummerMIS.ID);
    // 签核代理用户列表，代理多个用户以半角逗号隔开
    public static final String ProxyUsers = "ProxyUsers";
    // 客户端代码
    public static final String ClientIP = "clientIP";
    // 本地会话登录时间
    public static final String LoginTime = "loginTime";
    // 浏览器通用客户设备Id
    public static final String WebClient = "webclient";
    // 服务访问路径
    private static final String servicePath;
    // 产品静态文件
    public static final String productStatic;
    // spring context
    private static ApplicationContext context;
    @Deprecated
    public static final String clientIP = "clientIP";
    @Deprecated
    public static final String loginTime = "loginTime";
    @Deprecated
    public static final String roleCode = "roleCode";
    @Deprecated
    public static final String userCode = ISession.USER_CODE;

    static {
        productStatic = String.format("/%s/%s/common/cdn", ServerConfig.getAppProduct(), ServerConfig.getAppVersion());
        servicePath = config.getString("app.service.path", "");
    }

    // 图片静态路径
    public static String getStaticPath() {
        // zookeeper 路径 /diteng/main/common/cdn
        return ZkNode.get()
                .getNodeValue(productStatic, () -> config.getString("app.static.path", "http://oss.diteng.top/static"));
    }

    // aui静态资源路径
    public static String getAuiPath(String path) {
        return config.getString("aui.path", "js/aui") + "/" + path;
    }

    /**
     * 根据 application.xml 初始化 spring context
     * 
     * @return ApplicationContext context
     */
    public static ApplicationContext init() {
        initFromXml("application.xml");
        return context;
    }

    /**
     * 根据参数 springXmlFile 初始化 spring context
     * 
     * @param springXmlFile spring.xml文件
     * 
     * @return ApplicationContext context
     */
    public static ApplicationContext initFromXml(String springXmlFile) {
        if (context == null)
            setContext(new ClassPathXmlApplicationContext(springXmlFile));
        return context;
    }

    /**
     * 根据 SummerConfiguration.class 初始化 spring context
     * 
     * @return ApplicationContext context
     */
    public static ApplicationContext initOnlyFramework() {
        return init(SummerSpringConfiguration.class);
    }

    public static ApplicationContext init(Class<?>... annotatedClasses) {
        if (context == null) {
            // FIXME: 自定义作用域，临时解决 request, session 问题
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(annotatedClasses);
            RequestScope scope = new RequestScope();
            context.getBeanFactory().registerScope(RequestScope.REQUEST_SCOPE, scope);
            context.getBeanFactory().registerScope(RequestScope.SESSION_SCOPE, scope);

            setContext(context);
        }
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Application.setContext(applicationContext);
    }

    public static void setContext(ApplicationContext applicationContext) {
        if (context != applicationContext) {
            if (context != null) {
                log.error("applicationContext overload!");
            }
            context = applicationContext;
        }
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static boolean containsBean(Class<?> clazz) {
        if (context == null)
            return false;
        return context.getBeanNamesForType(clazz).length > 0;
    }

    public static boolean containsBean(String name) {
        if (context == null)
            return false;
        return context.containsBean(name);
    }

    @Deprecated
    public static Object getBean(String beanId) {
        if (!context.containsBean(beanId))
            return null;
        return context.getBean(beanId);
    }

    public static <T> T getBean(String beanId, Class<T> requiredType) {
        if (!context.containsBean(beanId))
            return null;
        return context.getBean(beanId, requiredType);
    }

    public static <T> T getBean(Class<T> requiredType) {
        if (context == null) {
            var e = new RuntimeException("context is null, getBean return null: " + requiredType.getSimpleName());
            log.error(e.getMessage(), e);
            return null;
        }
        try {
            return context.getBean(requiredType);
        } catch (BeansException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Deprecated
    public static ISession getSession() {
        return getBean(ISession.class);
    }

    @Deprecated
    public static IAppConfig getConfig() {
        return getBean(IAppConfig.class);
    }

    public static ISystemTable getSystemTable() {
        return getBean(ISystemTable.class);
    }

    public static <T> T getBean(IHandle handle, Class<T> requiredType) {
        T bean = getBean(requiredType);
        if (bean instanceof IHandle temp)
            temp.setSession(handle.getSession());
        return bean;
    }

    @Deprecated
    public static <T> T getBean(ISession session, Class<T> requiredType) {
        if (context.getBeanNamesForType(requiredType).length == 0)
            return null;
        T bean = context.getBean(requiredType);
        if ((session != null) && (bean instanceof IHandle))
            ((IHandle) bean).setSession(session);
        return bean;
    }

    public static Object getBean(IHandle handle, String beanId) {
        Object bean = getBean(beanId, Object.class);
        if (bean instanceof IHandle temp)
            temp.setSession(handle.getSession());
        return bean;
    }

    /**
     * 返回指定的service对象，若为空时会抛出 ClassNotFoundException
     * 
     * @param handle      IHandle
     * @param serviceCode 服务代码
     * @param function    KeyValue
     * @return Service bean
     * @throws ClassNotFoundException 类文件异常
     */
    public static IService getService(IHandle handle, String serviceCode, Variant function)
            throws ClassNotFoundException {
        if (Utils.isEmpty(serviceCode))
            throw new ClassNotFoundException("serviceCode is null.");

        // 读取xml中的配置
        IService bean = null;
        if (context.containsBean(serviceCode)) {
            bean = context.getBean(serviceCode, IService.class);
        } else {
            // 读取注解的配置，并自动将第一个字母改为小写
            String[] params = serviceCode.split("\\.");
            // 支持指定执行函数
            if (params.length > 1)
                function.setValue(params[1]);
            String beanId = getBeanIdOfClassCode(params[0]);
            if (context.containsBean(beanId))
                bean = context.getBean(beanId, IService.class);
            else {
                ISupplierService supplierService = context.getBean(ISupplierService.class);
                if (supplierService != null)
                    bean = supplierService.findService(handle, serviceCode);
                if (bean == null)
                    throw new ClassNotFoundException(String.format("bean %s not find", serviceCode));
            }
        }
        if (bean instanceof IHandle temp)
            temp.setSession(handle.getSession());
        return bean;
    }

    public static String getBeanIdOfClassCode(String classCode) {
        if (classCode.length() < 2)
            return classCode.toLowerCase();
        var temp = classCode;
        var first = classCode.substring(0, 2);
        if (!first.toUpperCase().equals(first))
            temp = classCode.substring(0, 1).toLowerCase() + classCode.substring(1);
        return temp;
    }

    /**
     * 获取应用级的语言类型
     * 
     * @return languageId
     */
    public static String getLanguageId() {
        String lang = ServerConfig.getInstance().getProperty(ISession.LANGUAGE_ID);
        if (lang == null || "".equals(lang) || LanguageResource.appLanguage.equals(lang)) {
            return LanguageResource.appLanguage;
        } else if (LanguageResource.LANGUAGE_EN.equals(lang)) {
            return lang;
        } else {
            throw new RuntimeException("not support language: " + lang);
        }
    }

    public static String getServicePath() {
        return servicePath;
    }

    public static boolean enableTaskService() {
        if (context == null)
            return false;
        return config.getBoolean("task.service", false);
    }

}
