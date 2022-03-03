package cn.cerc.mis.excel.output;

import cn.cerc.db.core.IHandle;

public interface IAccreditManager {

    /**
     * @param handle 环境参数
     * @return 返回是否可以通过本次权限
     */
    boolean isPass(IHandle handle);

    /**
     * @return 返回需要授权的权限描述
     */
    String getDescribe();
}
