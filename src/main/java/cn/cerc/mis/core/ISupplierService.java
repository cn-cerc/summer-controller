package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;

public interface ISupplierService {

    IService findService(IHandle handle, String serviceId);

}
