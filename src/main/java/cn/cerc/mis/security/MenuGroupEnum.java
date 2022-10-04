package cn.cerc.mis.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定义菜单分组
 * 
 * @author ZhangGong
 *
 */
public enum MenuGroupEnum {

    基本设置, 日常操作, 管理报表, 其它工具, 定制菜单, 自制菜单, 选购菜单, 停用菜单, 管理模组;

    public static String[] getNames() {
        List<String> list = new ArrayList<>();
        for (MenuGroupEnum k : MenuGroupEnum.values()) {
            list.add(k.name());
        }
        String[] names = new String[list.size()];
        return list.toArray(names);
    }

    public static Map<String, String> getItems() {
        Map<String, String> items = new LinkedHashMap<>();
        for (MenuGroupEnum item : MenuGroupEnum.values()) {
            items.put(String.valueOf(item.ordinal()), item.name());
        }
        return items;
    }

}
