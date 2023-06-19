package cn.cerc.mis.custom;

import java.util.ArrayList;
import java.util.List;

public interface ModuleFormImpl extends CustomFormImpl {
    /**
     * 
     * @return 取得模组说明
     */
    String getModuleReadme();

    /**
     * 实际可用菜单比这个少的，删除这个列表<br>
     * 实际可用菜单比这个多的，显示在这个返回值的后面
     * 
     * @return 菜单排序建议
     */
    default List<String> getMenuOrder() {
        return new ArrayList<String>();
    }
}
