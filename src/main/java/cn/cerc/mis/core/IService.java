package cn.cerc.mis.core;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.log.JayunLogParser;
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

    default DataSet _call(IHandle handle, DataSet dataIn, Variant function) {
        long startTime = System.currentTimeMillis();
        try {
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
                Throwable throwable = e.getCause() != null ? e.getCause() : e;
                if (throwable instanceof ServiceException || throwable instanceof RuntimeException
                        || throwable instanceof Error) {
                    String serviceCode = this.getClass().getName();
                    String message = String.format("service %s, corpNo %s, dataIn %s, message %s", function.key(),
                            handle.getCorpNo(), dataIn.json(), throwable.getMessage(), throwable);
                    LastModified modified = this.getClass().getAnnotation(LastModified.class);
                    // 自定义日志异常信息
                    JayunLogParser.analyze(handle, serviceCode, modified, throwable, message);
                    log.info("{}", message, throwable);
                } else {
                    if (!(throwable instanceof DataException))
                        log.error(throwable.getMessage(), throwable);
                }
                DataSet dataOut = new DataSet().setMessage(throwable.getMessage());
                return dataOut.setState(ServiceState.ERROR);
            }
        } finally {
            writeExecuteTime(handle, dataIn, function.key(), startTime);
        }
    }

    // 仅用于 Delphi Client 调用
    @Deprecated
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.json());
    }

    default void writeExecuteTime(IHandle handle, DataSet dataIn, String funcCode, long startTime) {
        var context = Application.getContext();
        if (context.getBeanNamesForType(IPerformanceMonitor.class).length == 0)
            return;
        var bean = context.getBean(IPerformanceMonitor.class);
        bean.writeServiceExecuteTime(handle, this, dataIn, funcCode, startTime);
    }

}
