package cn.cerc.mis.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.MD5;
import cn.cerc.db.core.ServerConfig;
import cn.cerc.db.core.Variant;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.db.redis.Redis;
import cn.cerc.mis.client.IServiceProxy;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

public class LocalService extends CustomServiceProxy implements IServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(LocalService.class);
    // 是否激活缓存
    private boolean bufferRead = true;
    private boolean bufferWrite = true;

    public LocalService(IHandle handle) {
        super(handle);
        String pageNo = null;
        HttpServletRequest req = (HttpServletRequest) handle.getSession().getProperty("request");
        if (req != null)
            pageNo = req.getParameter("pageno");

        // 遇到分页符时，尝试读取缓存
        this.bufferRead = pageNo != null;
    }

    public LocalService(IHandle handle, String service) {
        this(handle);
        this.setService(service);
    }

    // 带缓存调用服务
    @Override
    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = dataIn().head();
            if (args.length % 2 != 0) {
                // TODO 此处应该使用 ClassResource
                throw new RuntimeException("传入的参数数量必须为偶数！");
            }
            for (int i = 0; i < args.length; i = i + 2) {
                headIn.setValue(args[i].toString(), args[i + 1]);
            }
        }

        Variant function = new Variant("execute").setTag(service());
        Object object = getServiceObject(function);
        if (object == null)
            return false;

        try {
            if (object instanceof IHandle)
                ((IHandle) object).setSession(this.getSession());
            if (ServerConfig.isServerMaster()) {
                setDataOut(((IService) object)._call(this, dataIn(), function));
                return dataOut().state() > 0;
            }

            // 制作临时缓存Key
            String key = MD5.get(this.getUserCode() + this.service() + dataIn().json());

            if (bufferRead) {
                String buffValue = Redis.get(key);
                if (buffValue != null) {
                    log.debug("read from buffer: " + this.service());
                    DataSet dataOut = dataOut();
                    dataOut.setJson(buffValue);
                    return dataOut.state() > 0;
                }
            }

            // 没有缓存时，直接读取并存入缓存
            setDataOut(((IService) object)._call(this, dataIn(), function));
            if (bufferWrite) {
                log.debug("write to buffer: " + this.service());
                try (Jedis jedis = JedisFactory.getJedis()) {
                    if (jedis != null) {
                        jedis.set(key, dataOut().toString());
                        jedis.expire(key, 3600);
                    }
                }
            }
            return dataOut().state() > 0;
        } catch (Exception e) {
            Throwable err = e;
            if (e.getCause() != null)
                err = e.getCause();
            log.error(err.getMessage(), err);
            dataOut().setState(ServiceState.ERROR).setMessage(err.getMessage());
            return false;
        }
    }

    public String getExportKey() {
        String tmp = "" + System.currentTimeMillis();
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, this.getUserCode(), tmp)) {
            buff.setValue("data", this.dataIn().json());
        }
        return tmp;
    }

    public LocalService setBufferRead(boolean bufferRead) {
        this.bufferRead = bufferRead;
        return this;
    }

    public LocalService setBufferWrite(boolean bufferWrite) {
        this.bufferWrite = bufferWrite;
        return this;
    }

    @Deprecated
    public static void listMethod(Class<?> clazz) {
        Map<String, Class<?>> items = new HashMap<>();
        String[] args = clazz.getName().split("\\.");
        String classCode = args[args.length - 1];
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if ("boolean".equals(method.getReturnType().getName())) {
                if (method.getParameters().length == 0) {
                    String name = method.getName();
                    if (method.getName().startsWith("_")) {
                        name = name.substring(1);
                    }
                    items.put(classCode + "." + name, clazz);
                }
            }
        }
        for (String key : items.keySet()) {
            log.info(key);
        }
    }

}
