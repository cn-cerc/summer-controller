package cn.cerc.mis.client;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.db.core.Variant;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IService;

@Deprecated
public class LocalServer {

    public static DataSet call(ServiceSign service, IHandle handle, DataSet dataIn) {
        try {
            Variant function = new Variant("execute").setKey(service.id());
            IService bean = Application.getService(handle, service.id(), function);
            return bean._call(handle, dataIn, function);
        } catch (ClassNotFoundException e) {
            return new DataSet().setMessage("not find service: " + service.id());
        } catch (ServiceException e) {
            return new DataSet().setMessage(e.getMessage());
        }
    }
}
