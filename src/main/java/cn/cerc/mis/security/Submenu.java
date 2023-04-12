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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Submenu {
    /**
     * 
     * @return 快捷子菜单名称
     */
    String value();

    /**
     * 
     * @return 显示次序
     */
    int order();
}
