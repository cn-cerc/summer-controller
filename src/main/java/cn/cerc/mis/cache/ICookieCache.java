package cn.cerc.mis.cache;

import cn.cerc.db.core.IHandle;

/**
 * 业务通用缓存
 */
public interface ICookieCache {

    void flush(IHandle handle);

}