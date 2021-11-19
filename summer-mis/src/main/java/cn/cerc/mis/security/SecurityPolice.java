package cn.cerc.mis.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.cerc.core.KeyValue;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IForm;
import cn.cerc.mis.core.SupportBeanName;

@Component
public class SecurityPolice {
    private static final Logger log = LoggerFactory.getLogger(SecurityPolice.class);

    public static boolean check(IHandle handle, Class<?> clazz, Object bean) {
        String[] path = clazz.getName().split("\\.");

        Permission permission = findPermission(clazz.getAnnotations());
        Operators operators = findOperators(clazz.getAnnotations());

        String value = getValue(handle, bean, permission, operators);
        boolean result = validate(handle.getSession().getPermissions(), value);

        if (log.isDebugEnabled()) {
            String beanId = path[path.length - 1];
            if (bean instanceof SupportBeanName)
                beanId = ((SupportBeanName) bean).getBeanName();
            log.debug("check Class:{} ${}={}", beanId, value, result ? "pass" : "stop");
        }
        return result;
    }

    public static boolean check(IHandle handle, Method method, Object bean) {
        Class<?> clazz = method.getDeclaringClass();
        Permission permission = findPermission(method.getAnnotations(), clazz.getAnnotations());
        Operators operators = findOperators(method.getAnnotations(), clazz.getAnnotations());
        if (log.isDebugEnabled()) {
            log.debug("{}.{}[permissions]={}", handle.getCorpNo(), handle.getUserCode(),
                    handle.getSession().getPermissions());
        }
        String value = getValue(handle, bean, permission, operators);
        boolean result = validate(handle.getSession().getPermissions(), value);
        if (log.isDebugEnabled()) {
            String[] path = clazz.getName().split("\\.");
            String beanId = path[path.length - 1];
            if (bean instanceof SupportBeanName)
                beanId = ((SupportBeanName) bean).getBeanName();
            log.debug("checkMethod:{}.{} ${}={}", beanId, method.getName(), value, result ? "pass" : "stop");
        }
        return result;
    }

    public static boolean check(IHandle handle, Enum<?> clazz, String operator) {
        OperatorData data = new OperatorData(clazz.name().replaceAll("_", "."), operator);
        return check(handle, data);
    }

    public static boolean check(IHandle handle, String permission, String operator) {
        return check(handle, new OperatorData(permission, operator));
    }

    public static boolean check(IHandle handle, OperatorData data) {
        boolean result = validate(handle.getSession().getPermissions(), data.toString());
        if (log.isDebugEnabled()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement el = stackTrace[2];
            String[] path = el.getClassName().split("\\.");
            String beanId = path[path.length - 1];
            log.debug("checkValue:{}.{} ${}={}", beanId, el.getMethodName(), data.toString(), result ? "pass" : "stop");
        }
        return result;
    }

    public static boolean validate(String permissions, String value) {
        if (value == null || "".equals(value))
            return true;
        if (value.startsWith(Permission.GUEST))
            return true;

        String values = permissions;
        if (Utils.isEmpty(values))
            values = Permission.GUEST;

        log.debug("validate:{} in {}", value, values);
        String version = null;
        int site = values.indexOf("#");
        if (site > -1) {
            String tmp = values;
            values = tmp.substring(0, site);
            // 取出当前版本标识, 值如：1
            version = tmp.substring(site + 1).trim();
        }
        String text = value;
        int point = value.indexOf("#");
        if (point > -1)
            text = value.substring(0, point);

        // 支持版本号比对
        if (site > -1 && point > -1) {
            // 取出授权版本列表，值如：1,3,
            String versions = value.substring(point + 1).trim();
            if (version.length() > 0 && versions.length() > 0) {
                boolean pass = false;
                for (String item : versions.split("\\,")) {
                    if (version.equals(item.trim())) {
                        pass = true;
                        break;
                    }
                }
                if (!pass) {
                    log.debug("检查版本授权不通过, {}:{}", version, versions);
                    return false;
                }
            }
        }

        if (Utils.isEmpty(values) || Utils.isEmpty(text))
            return true;

        if (text.equals(Permission.USERS))
            return !values.equals(Permission.GUEST);

        // 授权了ADMIN权限
        if (values.equals(Permission.ADMIN))
            return true;

        // 授权与要求的权限相同
        if (values.equals(text))
            return true;

        // 如果出现被限制的权限（以减号开头），反向检查
        for (String item : values.split(";")) {
            if (item.startsWith("-")) {
                if (compareMaster(text, item.substring(1)))
                    return false;
                if (compareDetail(text, item.substring(1)))
                    return false;
            }
        }

        // 正常检查
        for (String item : values.split(";")) {
            if (!item.startsWith("-")) {
                if (compareMaster(item, text))
                    return true;
                if (compareDetail(item, text))
                    return true;
            }
        }

        return false;
    }

    private static boolean compareMaster(String master, String request) {
        if (master.equals(Permission.ADMIN))
            return true;

        if (request.equals(Permission.GUEST) || request.equals(Permission.USERS) || request.equals(master))
            return true;

        // 支持用户的授权带*使用
        if (master.endsWith(".*")) {
            String flag = master.substring(0, master.length() - 2);
            if (request.length() >= flag.length()) {
                if (request.substring(0, flag.length()).equals(flag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean compareDetail(String master, String request) {
        // 检查是否存在[]
        String masterText = master;
        int masterStart = master.indexOf("[");
        String[] masterDetail = {};
        if ((masterStart > 0) && master.endsWith("]")) {
            masterText = master.substring(0, masterStart);
            masterDetail = master.substring(masterStart + 1, master.length() - 1).split(",");
        }
        String childText = request;
        int childStart = request.indexOf("[");
        String[] childDetail = {};
        if ((childStart > 0) && request.endsWith("]")) {
            childText = request.substring(0, childStart);
            childDetail = request.substring(childStart + 1, request.length() - 1).split(",");
        }

        // 主体比较通过则继续比较
        if (compareMaster(masterText, childText)) {
            // 有任一方没有内容均视为通过
            if ((masterDetail.length == 0) || (childDetail.length == 0))
                return true;
            // 授权方仅有一个参数，且参数内容为*则视为通过
            if ((masterDetail.length) == 1 && (masterDetail[0].equals("*")))
                return true;

            // 比较detail
            boolean pass = true;
            for (String detail : childDetail) {
                if ((!"".equals(detail)) && (!inArray(detail, masterDetail))) {
                    pass = false;
                    break;
                }
            }
            if (pass)
                return true;
        }
        return false;
    }

    private static boolean inArray(final String value, final String[] list) {
        for (String item : list) {
            if (value.equals(item))
                return true;
        }
        return false;
    }

    private static String getValue(IHandle handle, Object bean, Permission permission, Operators operators) {
        String result = "";
        String defaultValue = Permission.USERS;
        if (permission != null) {
            result = permission.value();
            if (!"".equals(result)) {
                if (operators != null) {
                    StringBuilder sb = new StringBuilder(result);
                    sb.append("[");
                    int count = 0;
                    for (String detail : operators.value()) {
                        if (count > 0)
                            sb.append(",");
                        sb.append(detail);
                        count++;
                    }
                    sb.append("]");
                    result = sb.toString();
                }
            }
        } else if (handle != null) {
            if (bean instanceof IForm) {
                IForm form = (IForm) bean;
                String verlist = form.getParam("verlist", "");// TODO: 2021/11/19 增加独立方法获取，扫描注解、xml、mysql 
                result = form.getPermission();
                if (!Utils.isEmpty(verlist)) {
                    result = result + "#" + verlist;
                }
            }
            if ("".equals(result)) {
                String beanId;
                if (bean instanceof SupportBeanName) {
                    beanId = ((SupportBeanName) bean).getBeanName();
                    defaultValue = Permission.ADMIN;
                    SecurityService security = Application.getBean(SecurityService.class);
                    if (security != null) {
                        KeyValue outParam = new KeyValue().setKey(beanId);
                        security.loadPermission(handle, outParam);
                        result = outParam.asString();
                    }
                }
            }
        }
        return "".equals(result) ? defaultValue : result;
    }

    private static Permission findPermission(Annotation[]... list) {
        for (Annotation[] items : list) {
            for (Annotation item : items) {
                if (item instanceof Permission)
                    return (Permission) item;
            }
        }
        return null;
    }

    private static Operators findOperators(Annotation[]... list) {
        for (Annotation[] items : list) {
            for (Annotation item : items) {
                if (item instanceof Operators)
                    return (Operators) item;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String permissions = "users#4";
        String value = "base.product.manage#4,2,";
        System.out.println(SecurityPolice.validate(permissions, value));
    }

}
