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

    MenuGroupEnum group() default MenuGroupEnum.日常操作;

    String name();

    String parent();

    boolean appStore() default false;

}
