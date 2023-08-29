package cn.cerc.mis.core;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 最后一次修改
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface LastModified {

    /**
     * @return 修改人
     */
    String name();

    /**
     * @return 修改时间
     */
    String date();

}
