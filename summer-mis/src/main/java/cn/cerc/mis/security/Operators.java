package cn.cerc.mis.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Operators {
    // 增加、导入数据
    final String INSERT = "insert";
    // 修改数据
    final String UPDATE = "update";
    // 删除数据
    final String DELETE = "delete";

    // 审核
    final String FINISH = "finish";
    // 撤消
    final String CANCEL = "cancel";
    // 作废
    final String NULLIFY = "nullify";

    // 打印机报表输出、发送邮件报表
    final String REPORT = "report";
    // 数据导出权限
    final String EXPORT = "export";
    // 界面设置、报表设置
    final String DESIGN = "design";

    String[] value() default {};
}
