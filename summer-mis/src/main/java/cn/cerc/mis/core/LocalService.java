package cn.cerc.mis.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.DataSet;
import cn.cerc.core.MD5;
import cn.cerc.core.DataRow;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServerConfig;
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
            DataRow headIn = getDataIn().getHead();
            if (args.length % 2 != 0) {
                // TODO 此处应该使用 ClassResource
                throw new RuntimeException("传入的参数数量必须为偶数！");
            }
            for (int i = 0; i < args.length; i = i + 2) {
                headIn.setField(args[i].toString(), args[i + 1]);
            }
        }
    
        Object object = getServiceObject();
        if (object == null)
            return false;

        try {
            if (!"SvrSession.byUserCode".equals(this.getService()))
                log.debug(this.getService());
            if (object instanceof IHandle)
                ((IHandle) object).setSession(this.getSession());
            if (ServerConfig.isServerMaster()) {
                setDataOut(((IService) object).execute(this, getDataIn()));
                return getDataOut().getState() > 0;
            }

            // 制作临时缓存Key
            String key = MD5.get(this.getUserCode() + this.getService() + getDataIn().toJson());

            if (bufferRead) {
                String buffValue = Redis.get(key);
                if (buffValue != null) {
                    log.debug("read from buffer: " + this.getService());
                    DataSet dataOut = getDataOut();
                    dataOut.fromJson(buffValue);
                    return dataOut.getState() > 0;
                }
            }

            // 没有缓存时，直接读取并存入缓存
            setDataOut(((IService) object).execute(this, getDataIn()));
            if (bufferWrite) {
                log.debug("write to buffer: " + this.getService());
                try (Jedis jedis = JedisFactory.getJedis()) {
                    if (jedis != null) {
                        jedis.set(key, getDataOut().toString());
                        jedis.expire(key, 3600);
                    }
                }
            }
            return getDataOut().getState() > 0;
        } catch (Exception e) {
            Throwable err = e;
            if (e.getCause() != null) 
                err = e.getCause();
            log.error(err.getMessage(), err);
            getDataOut().setState(ServiceState.ERROR).setMessage(err.getMessage());
            return false;
        }
    }

    public String getExportKey() {
        String tmp = "" + System.currentTimeMillis();
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, this.getUserCode(), tmp)) {
            buff.setField("data", this.getDataIn().toJson());
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
