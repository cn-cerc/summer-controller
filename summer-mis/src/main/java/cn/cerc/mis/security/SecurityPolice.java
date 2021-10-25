package cn.cerc.mis.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.cerc.core.DataSet;
import cn.cerc.core.ISession;
import cn.cerc.core.KeyValue;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceException;
import cn.cerc.mis.core.ServiceState;

@Component
public class SecurityPolice {
    private static final Logger log = LoggerFactory.getLogger(SecurityPolice.class);

    public final boolean checkClass(IHandle sender) {
        return checkClass(sender, sender.getClass());
    }

    public final boolean checkClass(IHandle handle, Object sender) {
        return checkClass(handle, sender.getClass());
    }

    public final boolean checkClass(IHandle handle, Class<?> clazz) {
        String value = handle.getSession().getPermissions();
        String child = getPermission(clazz, handle);
        boolean result = checkValue(value, child);
//        if ("1310010010".equals(handle.getUserCode())) {
//            if (!result) {
//                log.warn("checkClass, {}:{}", value, child);
//                log.warn("{}, check class:{}", clazz.getName(), result ? "pass" : "stop");
//            }
//        }
        log.debug("{}, check class:{}", clazz.getName(), result ? "pass" : "stop");
        return result;
    }

    public final boolean checkMethod(IHandle handle, Method method) {
        return checkMethod(handle, handle.getClass(), method);
    }

    public final boolean checkMethod(IHandle handle, Class<?> clazz, Method method) {
        boolean find = false;
        String permission = Permission.USERS;
        for (Annotation item : method.getDeclaredAnnotations()) {
            if (item instanceof Permission) {
                permission = ((Permission) item).value();
                if ("".equals(permission))
                    permission = Permission.USERS;
                find = true;
            }
        }
        if (!find) {
            for (Annotation item : clazz.getDeclaredAnnotations()) {
                if (item instanceof Permission) {
                    permission = ((Permission) item).value();
                    if ("".equals(permission))
                        permission = Permission.USERS;
                    find = true;
                }
            }
        }

        boolean result;
        if (find) {
            for (Annotation item : method.getDeclaredAnnotations()) {
                if (item instanceof Operators) {
                    StringBuffer sb = new StringBuffer(permission);
                    sb.append("[");
                    int count = 0;
                    for (String detail : ((Operators) item).value()) {
                        if (count > 0)
                            sb.append(",");
                        sb.append(detail);
                        count++;
                    }
                    sb.append("]");
                    permission = sb.toString();
                }
            }
            result = this.checkValue(handle.getSession().getPermissions(), permission);
            log.debug("{}.{}, check method:{}", clazz.getName(), method.getName(), result ? "pass" : "stop");
        } else {
            result = this.checkClass(handle, clazz);
        }

        return result;
    }

    public final boolean checkValue(String permissions, String value) {
        log.debug("{}:{}", value, permissions);
        if (permissions == null)
            return true;
        if (value == null)
            return true;
        String values = permissions;
        int site = permissions.indexOf("#");
        if (site > -1)
            values = permissions.substring(0, site);
        String text = value;
        int point = value.indexOf("#");
        if (point > -1)
            text = value.substring(0, point);
        // 支持版本号比对
        if (site > -1 && point > -1) {
            // 取出当前版本标识, 值如：1
            String version = permissions.substring(site + 1, permissions.length()).trim();
            // 取出授权版本列表，值如：1,3,
            String versions = value.substring(point + 1, value.length()).trim();
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

    public DataSet call(IHandle handle, IService bean, DataSet dataIn, KeyValue function) throws ServiceException {
        String permission = getPermission(bean.getClass(), handle);
        if (this.allowGuestUser(permission))
            return bean.call(handle, dataIn, function);

        ISession session = handle.getSession();
        if ((session == null) || (!session.logon()))
            return new DataSet().setMessage("请您先登入系统").setState(ServiceState.ACCESS_DISABLED);

        // 检查权限代码是否匹配
        if (!this.checkValue(handle.getSession().getPermissions(), permission))
            return new DataSet().setMessage("您的执行权限不足").setState(ServiceState.ACCESS_DISABLED);

        return bean.call(handle, dataIn, function);
    }

    private final boolean compareMaster(String master, String request) {
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

    private final boolean allowGuestUser(String permission) {
        if (Permission.GUEST.length() > permission.length())
            return false;

        return permission.startsWith(Permission.GUEST);
    }

    private final boolean compareDetail(String master, String request) {
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

    private final boolean inArray(final String value, final String[] list) {
        for (String item : list) {
            if (value.equals(item))
                return true;
        }
        return false;
    }

    private final String getPermission(Class<?> clazz, IHandle handle) {
        boolean find = false;
        String permission = Permission.USERS;
        for (Annotation item : clazz.getDeclaredAnnotations()) {
            if (item instanceof Permission) {
                permission = ((Permission) item).value();
                if ("".equals(permission))
                    permission = Permission.USERS;
                find = true;
            }
        }
        if (find) {
            for (Annotation item : clazz.getDeclaredAnnotations()) {
                if (item instanceof Operators) {
                    StringBuffer sb = new StringBuffer(permission);
                    sb.append("[");
                    int count = 0;
                    for (String detail : ((Operators) item).value()) {
                        if (count > 0)
                            sb.append(",");
                        sb.append(detail);
                        count++;
                    }
                    sb.append("]");
                    permission = sb.toString();
                }
            }
        } else if (handle != null) {
            SecurityService security = Application.getBean(SecurityService.class);
            if (security != null) {
                String[] path = clazz.getName().split("\\.");
                KeyValue outParam = new KeyValue(permission).key(path[path.length - 1]);
                security.loadPermission(handle, outParam);
                permission = outParam.asString();
            }
        }

        log.debug("{}={}", clazz.getName(), permission);
        return permission;
    }

    public static void main(String[] args) {
        String permissions = "base.account.update;base.default;base.product.manage;other.addressbook;other.product.repair;other.vi"
                + "pcard.manage;sell.base.manage[insert,update,delete,nullify];sell.discount.manage;sell.order.wholesal"
                + "e[insert,update,delete,final,cancel,nullify];sell.report.process[export];sell.report.total[export];s"
                + "ell.stock.out.retail[insert,update,delete,final,cancel,nullify];sell.stock.out.scanner[insert,update"
                + ",delete,final,cancel,nullify];sell.stock.out.wholesale[insert,update,delete,final,cancel,nullify];"
                + "sell.stock.return[insert,update,delete,final,cancel,nullify];stock.report.inout";
        SecurityPolice police = new SecurityPolice();
        System.out.println(police.checkValue(permissions, "sell.stock.return"));
        System.out.println(police.checkValue(permissions, "sell.stock.return[insert]"));
    }
}
