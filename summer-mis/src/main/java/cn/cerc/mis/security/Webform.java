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

    String value() default "";

    String name();

    String module();

    String parent();

    String versions();

    String security() default "";

    String hide() default "";

    String custom() default "";

    String describe() default "";

}