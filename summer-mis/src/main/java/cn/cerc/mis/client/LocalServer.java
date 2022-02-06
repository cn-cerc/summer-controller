package cn.cerc.mis.client;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;

public class LocalServer implements IServiceServer {

    @Override
    public String getRequestUrl(IHandle handle, String service) {
        return null;
    }

    @Override
    public String getToken(IHandle handle) {
        return null;
    }

    @Override
    public DataSet _call(IHandle handle, DataSet dataIn, String serviceId) {
        try {
            Variant function = new Variant("execute").setTag(serviceId);
            IService bean = Application.getService(handle, serviceId, function);
            return bean._call(handle, dataIn, function);
        } catch (ClassNotFoundException e) {
            return new DataSet().setMessage("not find service: " + serviceId);
        } catch (ServiceException e) {
            return new DataSet().setMessage(e.getMessage());
        }
    }
}
