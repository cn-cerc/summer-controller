package cn.cerc.mis.client;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceState;

public class ServiceQuery {
    private static final Logger log = LoggerFactory.getLogger(ServiceQuery.class);
    private static final ClassConfig config = new ClassConfig(ServiceQuery.class, SummerMIS.ID);
    private DataSet dataOut;
    private ServiceSign service;

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataSet dataIn) {
        return new ServiceQuery(handle, service, dataIn);
    }

    public static ServiceQuery open(IHandle handle, ServiceSign service, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, service, dataIn);
    }

    public static ServiceQuery open(IHandle handle, ServiceSign service, Map<String, Object> headIn) {
        Objects.requireNonNull(headIn);
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return new ServiceQuery(handle, service, dataIn);
    }

    public static ServiceQuery call(IHandle handle, ServiceSign service, DataSet dataIn) {
        return new ServiceQuery(handle, service, dataIn);
    }

    public static ServiceQuery call(IHandle handle, ServiceSign service, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, service, dataIn);
    }

    public static ServiceQuery call(IHandle handle, ServiceSign service, Map<String, Object> headIn) {
        Objects.requireNonNull(headIn);
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return new ServiceQuery(handle, service, dataIn);
    }

    private ServiceQuery(IHandle handle, ServiceSign service, DataSet dataIn) {
        this.service = service;
        dataOut = execute(handle, dataIn);
    }

    private DataSet execute(IHandle handle, DataSet dataIn) {
        try {
            if (service.site() == null) {
                Variant function = new Variant("execute").setTag(service.id());
                IService bean = Application.getService(handle, service.id(), function);
                return bean._call(handle, dataIn, function);
            } else {
                return call(handle, dataIn, handle.getSession().getToken());
            }
        } catch (ClassNotFoundException e) {
            return new DataSet().setMessage("not find service: " + service.id());
        } catch (ServiceException e) {
            return new DataSet().setMessage(e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            return new DataSet().setMessage(e.getMessage());
        }
    }

    private DataSet call(IHandle handle, DataSet dataIn, String token) {
        Objects.requireNonNull(service.site());
        try {
            String host = config.getClassProperty("site." + service.site(), null);
            String url = String.format("%s?service=%s", host, service.id());
            String sid = token;
            if (sid == null)
                sid = config.getClassProperty("site." + service.site() + ".token", null);

            Curl curl = new Curl();
            if (sid != null)
                curl.put(ISession.TOKEN, sid);
            curl.put("dataIn", dataIn.json());
            log.debug("request url: {}", url);
            log.debug("request params: {}", curl.getParameters());
            String response = curl.doPost(url);
            log.debug("response: {}", response);
            return new DataSet().setJson(response);
        } catch (IOException e) {
            return new DataSet().setState(ServiceState.CALL_TIMEOUT).setMessage("remote service error");
        } catch (Throwable e) {
            e.printStackTrace();
            return new DataSet().setMessage(e.getMessage());
        }
    }

    public boolean isOk() {
        return dataOut.state() > 0;
    }

    public boolean isFail() {
        return dataOut.state() <= 0;
    }

    public DataSet get() {
        return dataOut;
    }

    public DataSet getElseThrow() throws ServiceQueryException {
        if (dataOut.state() <= 0)
            throw new ServiceQueryException(dataOut.message());
        return dataOut;
    }

    public <X extends Throwable> DataSet getElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (dataOut.state() <= 0)
            throw exceptionSupplier.get();
        return dataOut;
    }

}
