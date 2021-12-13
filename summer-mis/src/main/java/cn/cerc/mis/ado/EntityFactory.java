package cn.cerc.mis.ado;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Entity;

import org.springframework.context.ApplicationContext;

import cn.cerc.core.SqlServer;
import cn.cerc.core.SqlServerType;
import cn.cerc.mis.core.Application;

public class EntityFactory {
    private static ConcurrentMap<String, Class<? extends AdoTable>> items = new ConcurrentHashMap<>();

    public static Class<? extends AdoTable> findEntityClass(String table, SqlServerType sqlServerType) {
        ApplicationContext context = Application.getContext();
        if (context == null)
            return null;
        if (items != null)
            return items.get(table);

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
                        items.put(entity.name(), clazz);
                    else
                        items.put(clazz.getSimpleName(), clazz);
                }
            }
        }

        return items.get(table);
    }
}
