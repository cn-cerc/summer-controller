package cn.cerc.mis.client;

import cn.cerc.db.core.IHandle;

public interface IServiceServer {

	String getRequestUrl(String service);
	
	String getToken(IHandle handle);
}
