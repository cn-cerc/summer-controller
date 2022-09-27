package cn.cerc.mis.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Webform {

    int id() default 0;

    String module();

    String name();

    MenuGroupEnum group();// default MenuGroupEnum.日常操作;

    boolean appStore() default false;

    // 是否需要自动帮助中心
    boolean autoHelper() default true;
}
