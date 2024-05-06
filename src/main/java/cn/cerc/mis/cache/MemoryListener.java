package cn.cerc.mis.cache;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.BasicHandle;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

/**
 * 内存缓存监听器
 */
@Component
@WebListener
public class MemoryListener implements ServletContextListener, HttpSessionListener {
    private static final Logger log = LoggerFactory.getLogger(MemoryListener.class);
    public static final String CacheChannel = MemoryBuffer.buildKey(SystemBuffer.Global.CacheReset);
    public static ApplicationContext context;
    private CacheResetMonitor monitor;
    private static final AtomicInteger atomic = new AtomicInteger();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        context = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());

        monitor = new CacheResetMonitor();
        monitor.setName("CacheReset-monitor");
        monitor.start();

        ApplicationContext context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(sce.getServletContext());
        if (context != null) {
            resetCache(context, CacheResetMode.Start);
        } else {
            log.error("application context null.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (monitor != null) {
            monitor.requestStop();
            monitor = null;
        }

        // 通知所有的单例重启缓存
        if (context == null)
            return;

        Application.setContext(context);
        for (String beanId : context.getBeanDefinitionNames()) {
            if (context.isSingleton(beanId)) {
                Object bean = context.getBean(beanId);
                if (bean instanceof IShutdown shutdown) {
                    shutdown.shutdown();
                }
            }
        }
    }

    @Override
    public synchronized void sessionCreated(HttpSessionEvent event) {
        atomic.incrementAndGet();
        if (atomic.get() % 10 == 0)
            log.debug("session current size: {}", atomic.get());
        log.debug("session MaxInactiveInterval: {}", event.getSession().getMaxInactiveInterval());
        log.debug("session: {}", event.getSession());
        // 过期时间设置，单位为秒
//        event.getSession().setMaxInactiveInterval(30);
    }

    @Override
    public synchronized void sessionDestroyed(HttpSessionEvent se) {
        log.debug("session: {}", se.getSession());
        log.debug("session MaxInactiveInterval: {}", se.getSession().getMaxInactiveInterval());
        atomic.decrementAndGet();
        if (atomic.get() % 10 == 0)
            log.debug("session current size: {}", atomic);

        if (atomic.get() != 0)
            return;

        ApplicationContext context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(se.getSession().getServletContext());
        if (context != null) {
            resetCache(context, CacheResetMode.Reset);
        } else {
            log.error("application context null.");
        }
    }

    private void resetCache(ApplicationContext context, CacheResetMode resetType) {
        // 通知所有的单例重启缓存
        Application.setContext(context);
        try (BasicHandle handle = new BasicHandle()) {
            for (String beanId : context.getBeanDefinitionNames()) {
                if (context.isSingleton(beanId)) {
                    Object bean = context.getBean(beanId);
                    if (bean instanceof IMemoryCache cache) {
                        cache.resetCache(handle, resetType, null);
                        log.debug("{}.resetCache", beanId);
                    }
                }
            }
        }
    }

    public static void refresh(Class<? extends IMemoryCache> clazz, String param) {
        IMemoryCache cache = Application.getBean(clazz);
        if (cache == null)
            return;
        final String beanId = cache.getBeanName();
        try (Jedis jedis = JedisFactory.getJedis()) {
            if (jedis != null)
                jedis.publish(CacheChannel, param == null ? beanId : (beanId + ":" + param));
        }
    }

}
