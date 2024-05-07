package cn.cerc.mis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.MD5;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Utils;
import cn.cerc.db.core.Variant;
import cn.cerc.db.redis.Redis;
import cn.cerc.mis.other.MemoryBuffer;
import cn.cerc.mis.security.Permission;

public interface IService {
    Logger log = LoggerFactory.getLogger(IService.class);

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

    default DataSet _call(IHandle handle, DataSet dataIn, Variant function) throws IllegalAccessException,
            InvocationTargetException, ServiceException, DataException, RuntimeException {
        // FIXME 去掉内存计算过滤器字段，使用原始 dataIn 进行数据查询
        if (dataIn.head().exists("_RecordFilter_")) {
            dataIn.head().fields().remove("_RecordFilter_");
        }
        long startTime = System.currentTimeMillis();
        String redisKey = null;
        ServiceCache cacheConfig = this.getClass().getAnnotation(ServiceCache.class);
        if (cacheConfig != null) {
            String level = switch (cacheConfig.level()) {
            case user -> handle.getUserCode();
            case corp -> handle.getCorpNo();
            case system -> "";
            default -> handle.getSession().getToken();
            };
            String md5 = MD5.get(level + function.getString() + dataIn.json());
            int prefix = MemoryBuffer.prefix(SystemBuffer.Service.Cache);
            redisKey = String.join(".", String.valueOf(prefix), this.getClass().getSimpleName(), md5);
            try (Redis redis = new Redis()) {
                String json = redis.get(redisKey);
                if (Utils.isNotEmpty(json)) {
                    return new DataSet().setJson(json);
                }
            }
        }

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
            if (this instanceof ServiceNameAwareImpl service) {
                String key = function.key();
                if (key != null && key.endsWith(".execute"))
                    function.setKey(key.substring(0, key.lastIndexOf(".execute")));
                service.setServiceId(function);
            }
            DataSet dataSet = sm.call(this, handle, dataIn);
            if (cacheConfig != null) {
                try (Redis redis = new Redis()) {
                    long seconds = cacheConfig.expire();
                    if (seconds < 3) {
                        seconds = 3L;
                        log.warn("{} 服务缓存过期时间不允许小于3秒，强制改为3秒", this.getClass().getSimpleName());
                    }
                    if (seconds > TimeUnit.HOURS.toSeconds(24)) {
                        seconds = TimeUnit.HOURS.toSeconds(24);
                        log.warn("{} 服务缓存过期时间不允许大于24小时，强制改为24小时", this.getClass().getSimpleName());
                    }
                    redis.setex(redisKey, seconds, dataSet.json());
                }
            }
            return dataSet;
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
