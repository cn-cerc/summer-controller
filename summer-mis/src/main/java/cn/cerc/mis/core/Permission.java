package cn.cerc.mis.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    // 匿名用户可用，不需要登录且放开一切管控，若Service有此标识，则代表此Service不需要登录即可调用
    String GUEST = "guest";
    // 默认值，代表此程序必须登录后方可使用，所有用户帐号均属于此类别！
    String USERS = "users";
    // 特殊用户：系统管理员，代表此Service仅系统管理员能够调用
    String ADMIN = "admin";
    // 特殊用户：仅供系统使用，如系统备份帐号system.backup；如系统服务商客服帐号建议为：system.service.provider
    String SYSTEM = "system";
    // 特殊用户：客户，可扩展如企业用户 customer.enterprise 或个人用户 customer.personal
    String CUSTOMER = "customer";
    // 其它命名建议，如财务经理 user.finance.manager

    // 当前授权码
    String value() default "";
}
