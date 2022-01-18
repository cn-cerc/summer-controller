package cn.cerc.mis.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IAppConfig;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.LanguageResource;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.SqlServer;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.ado.AdoTable;
import cn.cerc.mis.ado.EntityFactory;

@Component
public class Application implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final ClassConfig config = new ClassConfig(Application.class, SummerMIS.ID);
    private static ConcurrentMap<String, Class<? extends AdoTable>> entityItems = new ConcurrentHashMap<>();
//    public static final String TOKEN = ISession.TOKEN;
//    public static final String bookNo = ISession.CORP_NO;
//    public static final String userCode = ISession.USER_CODE;
//    public static final String userName = ISession.USER_NAME;
    // tomcat JSESSION.ID
    public static final String SessionId = "sessionId";
    // FIXME 如下2个常量需要取消其引用，改为直接使用ISession
//    @Deprecated
//    public static final String UserId = "UserID";
    // 签核代理用户列表，代理多个用户以半角逗号隔开
    public static final String ProxyUsers = "ProxyUsers";
    // 客户端代码
    public static final String ClientIP = "clientIP";
    // 本地会话登录时间
    public static final String LoginTime = "loginTime";
    // 浏览器通用客户设备Id
    public static final String WebClient = "webclient";
    // 图片静态路径
    private static String staticPath;
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
//    @Deprecated
//    public static final String userId = UserId;
//    @Deprecated
//    public static final String token = ISession.TOKEN;
//    @Deprecated
//    public static final String bookNo = ISession.CORP_NO;

    static {
        staticPath = config.getString("app.static.path", "");
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
        if (context == null) {
            // FIXME: 自定义作用域，临时解决 request, session 问题

            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                    SummerSpringConfiguration.class);
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

    public static <T> T getBean(Class<T> requiredType) {
        String[] beans = context.getBeanNamesForType(requiredType);
        if (beans.length == 0)
            return null;
        else if (beans.length == 1)
            return context.getBean(requiredType);
        else {
            String[] path = requiredType.getName().split("\\.");
            String beanId = path[path.length - 1];
            if (context.containsBean(beanId)) // 优先XML中注册的：id一般与类名相同
                return context.getBean(beanId, requiredType);
            else {
                beanId = null;
                for (String item : beans) {
                    if (!item.endsWith("Default"))
                        beanId = item;
                }
                if (beanId == null)
                    beanId = beans[0];
                return context.getBean(beanId, requiredType);
            }
        }
    }

    public static Object getBean(String beanId) {
        if (!context.containsBean(beanId))
            return null;
        return context.getBean(beanId);
    }

    public static ISession getSession() {
        return context.getBean(ISession.class);
    }

    public static IAppConfig getConfig() {
        return context.getBean(IAppConfig.class);
    }

    public static ISystemTable getSystemTable() {
        return context.getBean(ISystemTable.class);
    }

    public static <T> T getBean(IHandle handle, Class<T> requiredType) {
        if (context.getBeanNamesForType(requiredType).length == 0)
            return null;
        T bean = context.getBean(requiredType);
        if ((handle != null) && (bean instanceof IHandle))
            ((IHandle) bean).setSession(handle.getSession());
        return bean;
    }

    public static Object getBean(IHandle handle, String beanId) {
        if (!context.containsBean(beanId))
            return null;
        Object bean = context.getBean(beanId);
        if ((handle != null) && (bean instanceof IHandle))
            ((IHandle) bean).setSession(handle.getSession());
        return bean;
    }

    public static Object getBean(ISession session, String beanId) {
        if (!context.containsBean(beanId))
            return null;
        Object bean = context.getBean(beanId);
        if ((session != null) && (bean instanceof IHandle))
            ((IHandle) bean).setSession(session);
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
        Object bean = null;
        if (context.containsBean(serviceCode)) {
            bean = context.getBean(serviceCode, IService.class);
        } else {
            // 读取注解的配置，并自动将第一个字母改为小写
            String[] params = serviceCode.split("\\.");
            // 支持指定执行函数
            if (params.length > 1)
                function.setData(params[1]);

            String beanId = params[0];
            if (!beanId.substring(0, 2).toUpperCase().equals(beanId.substring(0, 2)))
                beanId = beanId.substring(0, 1).toLowerCase() + beanId.substring(1);
            if (context.containsBean(beanId))
                bean = context.getBean(beanId, IService.class);
            else
                throw new ClassNotFoundException(String.format("bean %s not find", serviceCode));
        }
        if (bean instanceof IHandle)
            ((IHandle) bean).setSession(handle.getSession());
        return (IService) bean;
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

    public static String getStaticPath() {
        return staticPath;
    }

    @Deprecated
    public static IHandle getHandle() {
        return new Handle(getSession());
    }

    public static Class<? extends AdoTable> searchClass(String table, SqlServerType sqlServerType) {
        ApplicationContext context = Application.getContext();
        if (context == null)
            return null;
        if (entityItems != null)
            return entityItems.get(table);

        synchronized (EntityFactory.class) {
            for (String beanId : context.getBeanNamesForType(AdoTable.class)) {
                Object bean = context.getBean(beanId);
                @SuppressWarnings("unchecked")
                Class<? extends AdoTable> clazz = (Class<? extends AdoTable>) bean.getClass();
                SqlServer server = clazz.getDeclaredAnnotation(SqlServer.class);
                SqlServerType sst = server != null ? server.type() : SqlServerType.Mysql;
                if (sst == sqlServerType) {
                    Entity entity = clazz.getDeclaredAnnotation(Entity.class);
                    if (entity != null && !"".equals(entity.name()))
                        entityItems.put(entity.name(), clazz);
                    else
                        entityItems.put(clazz.getSimpleName(), clazz);
                }
            }
        }

        return entityItems.get(table);
    }

}
