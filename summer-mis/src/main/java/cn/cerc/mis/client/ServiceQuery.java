package cn.cerc.mis.client;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;

public class ServiceQuery {
    private DataSet dataOut;

    public static ServiceQuery open(IHandle handle, String serviceCode, DataSet dataIn) {
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    public static ServiceQuery open(IHandle handle, String serviceCode, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    public static ServiceQuery open(IHandle handle, String serviceCode, Map<String, Object> headIn) {
        Objects.requireNonNull(headIn);
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    public static ServiceQuery call(IHandle handle, String serviceCode, DataSet dataIn) {
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    public static ServiceQuery call(IHandle handle, String serviceCode, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    public static ServiceQuery call(IHandle handle, String serviceCode, Map<String, Object> headIn) {
        Objects.requireNonNull(headIn);
        DataSet dataIn = new DataSet();
        headIn.forEach((key, value) -> dataIn.head().setValue(key, value));
        return new ServiceQuery(handle, serviceCode, dataIn);
    }

    private ServiceQuery(IHandle handle, String serviceCode, DataSet dataIn) {
        try {
            Variant function = new Variant("execute").setTag(serviceCode);
            IService service = Application.getService(handle, serviceCode, function);
            dataOut = service._call(handle, dataIn, function);
        } catch (ClassNotFoundException e) {
            dataOut = new DataSet().setMessage("not find service: " + serviceCode);
        } catch (ServiceException e) {
            dataOut = new DataSet().setMessage(e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            dataOut = new DataSet().setMessage(e.getMessage());
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
