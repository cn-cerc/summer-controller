package cn.cerc.mis.ado;

public @interface Describe {

    String name();

    String remark() default "";

    int version() default 0;
}
