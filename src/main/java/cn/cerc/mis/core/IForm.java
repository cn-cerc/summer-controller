package cn.cerc.mis.core;

import javax.servlet.http.HttpServletRequest;

import cn.cerc.db.core.IHandle;
import cn.cerc.mis.security.Permission;

public interface IForm extends IHandle, IResponseOwner, IPermission, SupportBeanName {
    // 页面代码
    void setId(String formId);

    String getId();

    // 页面名称
    String getName();

    // 取得访问设备讯息
    AppClient getClient();

    // 设置参数
    void setParam(String key, String value);

    // 取得参数
    String getParam(String key, String def);

    // 输出页面（支持jsp、reddirect、json等）
    IPage execute() throws Exception;

    // 执行指定函数，并返回jsp文件名，若自行处理输出则直接返回null
    String _call(String funcId) throws Exception;

    void setPathVariables(String[] pathVariables);

//    @Deprecated
//    default Object getProperty(String key) {
//        return getSession().getProperty(key);
//    }

//    @Override
//    @Deprecated
//    default void setProperty(String key, Object value) {
//        getSession().setProperty(key, value);
//    }

    @Override
    HttpServletRequest getRequest();

    default boolean _isAllowGuest() {
        Permission ps = this.getClass().getAnnotation(Permission.class);
        return ps != null && Permission.GUEST.equals(ps.value());
    }

    default void writeExecuteTime(String funcCode, long startTime) {
        var context = Application.getContext();
        if (context.getBeanNamesForType(IPerformanceMonitor.class).length == 0)
            return;
        var bean = context.getBean(IPerformanceMonitor.class);
        bean.writeFormExecuteTime(this, funcCode, startTime);
    }

}
