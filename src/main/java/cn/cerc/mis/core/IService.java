package cn.cerc.mis.core;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.security.Permission;

public interface IService {
    public static final Logger log = LoggerFactory.getLogger(IService.class);

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
            ServiceMethod sm = ServiceMethod.build(this.getClass(), method.getName());
            if (sm != null) {
                dataOut.append();
                dataOut.setValue("code", method.getName());
                WebMethod el2 = method.getAnnotation(WebMethod.class);
                if (el2 != null)
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
        ServiceMethod sm = ServiceMethod.build(this.getClass(), funcCode);
        if (sm == null) {
            DataSet dataOut = new DataSet();
            dataOut.setMessage(String.format("%s.%s not find！", this.getClass().getName(), funcCode));
            return dataOut.setState(ServiceState.NOT_FIND_SERVICE);
        }

        // 执行具体的服务函数
        try {
            if (this instanceof ServiceNameAwareImpl service) {
                String key = function.key();
                if (key != null && key.endsWith(".execute"))
                    function.setKey(key.substring(0, key.lastIndexOf(".execute")));
                service.setServiceId(function);
            }
            return sm.call(this, handle, dataIn);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ServiceException)
                log.error("service {}, corpNo {}, dataIn {}, message {}", function.key(), handle.getCorpNo(),
                        dataIn.json(), cause.getMessage(), cause);
            DataSet dataOut = new DataSet().setMessage(cause.getMessage());
            return dataOut.setState(ServiceState.ERROR);
        }
    }

    // 仅用于 Delphi Client 调用
    @Deprecated
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.json());
    }

}
