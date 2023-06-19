package cn.cerc.mis.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标识子菜单，主要用于：增加、查询、统计，须与 Webform 组合使用
 * 
 * @author ZhangGong
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Submenu {
    /**
     * 
     * @return 返回父级菜单代码，可为空，其逻辑如下：<br>
     *         若标识为函数时，此值不应该设置，并默认等于其所在类<br>
     *         若标识为类时，默认等于Webform.module值，此值可被设置<br>
     *         注意：实际在使用时，数据库中可以分行业设置并覆盖此值
     * 
     */
    String parent();

    /**
     * 
     * @return 显示次序
     */
    int order();

    /**
     * 
     * @return 快捷子菜单名称
     */
    String subname();
}
