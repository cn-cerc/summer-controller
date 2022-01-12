package cn.cerc.mis.security;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.IService;

public interface SecurityService extends IService {

    /**
     * 根据token，初始化session
     * 
     * @param session 执行环境
     * @param token   授权token，亦称sid, 即session id
     * @return 若token合法，则返回true
     */
    boolean initSession(ISession session, String token);

    /**
     * 返回当前用户获得的授权代码
     * 
     * @param session 执行环境
     * @return 若当前用户为guest，请返回null，特殊定义请参考Permission中的常量
     */
    String getPermissions(ISession session);

    /**
     * 返回指定对象id需要的授权码, 主要用于在service未定义授权代码时，从数据库中取值
     * 
     * @param handle      执行环境
     * @param outParam: key = object id, value = 默认授权码
     */
    void loadPermission(IHandle handle, Variant outParam);
}
