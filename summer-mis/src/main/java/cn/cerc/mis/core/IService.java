package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.DataSet;
import cn.cerc.core.KeyValue;
import cn.cerc.core.Utils;
import cn.cerc.db.core.IHandle;

public interface IService {
    static final Logger _log = LoggerFactory.getLogger(IService.class);

    default DataSet call(IHandle handle, DataSet dataIn, KeyValue function) throws ServiceException {
        if (function == null || Utils.isEmpty(function.asString()))
            return new DataSet().setMessage("function is null");
        if ("call".equals(function.asString()))
            return new DataSet().setMessage("function is call");

        DataSet dataOut = new DataSet();
        String funcCode = function.asString();
        Class<?> self = this.getClass();
        Method method = null;
        for (Method item : self.getMethods()) {
            if (!item.getName().equals(funcCode))
                continue;
            if (item.getParameterCount() != 2)
                continue;
            if (!item.getParameters()[1].getType().equals(DataSet.class))
                continue;
            if (!item.getReturnType().equals(DataSet.class))
                continue;
            if (method != null) {
                dataOut.setMessage(
                        String.format("find two or more method: %s.%s ！", this.getClass().getName(), funcCode));
                return dataOut.setState(ServiceState.NOT_FIND_SERVICE);
            }
            method = item;
        }
        if (method == null) {
            dataOut.setMessage(String.format("not find service: %s.%s ！", this.getClass().getName(), funcCode));
            return dataOut.setState(ServiceState.NOT_FIND_SERVICE);
        }

        try {
            // 执行具体的服务函数
            dataOut = (DataSet) method.invoke(this, handle, dataIn);
            // 防止调用者修改并回写到数据库
            dataOut.disableStorage();
            dataOut.first();
            return dataOut;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Throwable err = e.getCause() != null ? e.getCause() : e;
            String msg = err.getMessage() == null ? "error is null" : err.getMessage();
            if ((err instanceof ServiceException)) {
                return new DataSet().setState(ServiceState.ERROR).setMessage(msg);
            } else {
                _log.error(msg, err);
                return new DataSet().setState(ServiceState.ERROR).setMessage(msg);
            }
        }
    }

    // 仅用于 Delphi Client 调用
    @Deprecated
    default String getJSON(DataSet dataOut) {
        return String.format("[%s]", dataOut.toJson());
    }

}
