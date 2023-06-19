package cn.cerc.mis.register.center;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@WebListener
public class ServletContextRegistryListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(ServletContextRegistryListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("{} contextInitialized.", this.getClass().getName());
        ZkLoad.get().register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("{} contextDestroyed.", this.getClass().getName());
        ZkLoad.get().unRegister();
    }

}
