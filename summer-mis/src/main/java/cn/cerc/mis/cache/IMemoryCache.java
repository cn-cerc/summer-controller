package cn.cerc.mis.cache;

import org.springframework.beans.factory.BeanNameAware;

import cn.cerc.db.core.IHandle;

/**
 * spring single scope cache
 */
public interface IMemoryCache extends BeanNameAware {

    void resetCache(IHandle handle, CacheResetMode resetType, String param);

    String getBeanName();

}
