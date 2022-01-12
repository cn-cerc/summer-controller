package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.security.Permission;
import cn.cerc.mis.security.SecurityPolice;
import cn.cerc.mis.security.SecurityStopException;

public interface IService {
    static final Logger _log = LoggerFactory.getLogger(IService.class);

    /**
     * 
     * @return 返回当前函数功能列表
     */
    default DataSet _list() {
        DataSet dataOut = new DataSet();
        Class<?> clazz = this.getClass();
        WebService el1 = clazz.getAnnotation(WebService.class);
        if (el1 != null)
            dataOut.head().setValue("describe", el1.describe());
        Permission ps1 = clazz.getAnnotation(Permission.class);
        if (ps1 != null)
            dataOut.head().setValue("permission", ps1.value());
        for (Method method : clazz.getMethods()) {
            WebMethod el2 = method.getAnnotation(WebMethod.class);
            if (el2 != null) {
                dataOut.append();
                dataOut.setValue("code", method.getName());
                dataOut.setValue("describe", el2.value());
                Permission ps2 = clazz.getAnnotation(Permission.class);
                if (ps2 != null)
                    dataOut.setValue("permission", ps2.value());
            }
        }
        return dataOut.setState(ServiceState.OK);
    }

    default DataSet _call(IHandle handle, DataSet dataIn, Variant function) throws ServiceException {
        if (function == null || Utils.isEmpty(function.getString()))
            return new DataSet().setMessage("function is null");
        if ("_call".equals(function.getString()))
            return new DataSet().setMessage("function is call");
        if ("_list".equals(function.getString())) {
            return _list();
        }

        String funcCode = function.getString();
        Class<?> clazz = this.getClass();
        Method method;
        Object[] args = null;
        try {
            method = clazz.getMethod(funcCode, IHandle.class, DataSet.class);
            args = new Object[2];
            args[0] = handle;
            args[1] = dataIn;
        } catch (NoSuchMethodException | SecurityException e1) {
            method = null;
        }
        if (method == null) {
            try {
                method = clazz.getMethod(funcCode, IHandle.class, DataRow.class);
                args = new Object[2];
                args[0] = handle;
                args[1] = dataIn.head();
            } catch (NoSuchMethodException | SecurityException e1) {
                method = null;
            }
        }
        if (method == null) {
            try {
                method = clazz.getMethod(funcCode, IHandle.class, DataSet.class, DataSet.class);
                args = new Object[3];
                args[0] = handle;
                args[1] = dataIn;
                args[2] = new DataSet();
                if (!boolean.class.isAssignableFrom(method.getReturnType()))
                    method = null;
            } catch (NoSuchMethodException | SecurityException e1) {
                method = null;
            }
        }
        if (method == null) {
            DataSet dataOut = new DataSet();
            dataOut.setMessage(String.format("%s.%s not find！", this.getClass().getName(), funcCode));
            return dataOut.setState(ServiceState.NOT_FIND_SERVICE);
        }

        // 执行具体的服务函数
        try {
            DataValidate validate = method.getDeclaredAnnotation(DataValidate.class);
            if (validate != null) {
                DataRow headIn = dataIn.head();
                String errorMsg = validate.message();
                for (String fieldCode : validate.value()) {
                    if (!headIn.has(fieldCode)) {
                        if (errorMsg.contains("%s"))
                            throw new DataValidateException(String.format(errorMsg, fieldCode));
                        else
                            throw new DataValidateException(errorMsg);
                    }
                }
            }
            if (!SecurityPolice.check(handle, method, this)) {
                DataSet dataOut = new DataSet();
                dataOut.setMessage(SecurityStopException.getAccessDisabled());
                return dataOut.setState(ServiceState.ACCESS_DISABLED);
            }
            // 开始执行具体函数
            if (DataSet.class.isAssignableFrom(method.getReturnType())) {
                DataSet dataOut = (DataSet) method.invoke(this, args);
                dataOut.disableStorage().first(); // 防止调用者修改并回写到数据库
                return dataOut;
            } else if (args.length == 3) {
                DataSet dataOut = (DataSet) args[2];
                boolean result = (boolean) method.invoke(this, args);
                dataOut.setState(result ? ServiceState.OK : ServiceState.ERROR);
                dataOut.disableStorage().first(); // 防止调用者修改并回写到数据库
                return dataOut;
            } else {
                DataSet dataOut = new DataSet();
                dataOut.setMessage(String.format("%s.%s returnType error！", this.getClass().getName(), funcCode));
                return dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Throwable err = e.getCause() != null ? e.getCause() : e;
            String msg = err.getMessage() == null ? "error is null" : err.getMessage();
            DataSet dataOut = new DataSet().setMessage(msg);
            if (!(err instanceof ServiceException))
                _log.error(msg, err);
            return dataOut.setState(ServiceState.ERROR);
        }
    }

    // 仅用于 Delphi Client 调用
    @Deprecated
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.json());
    }

}
