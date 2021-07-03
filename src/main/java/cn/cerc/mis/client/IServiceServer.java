package cn.cerc.mis.client;

public interface IServiceServer {

	String getRequestUrl(String service);
	
	String getToken();
}
