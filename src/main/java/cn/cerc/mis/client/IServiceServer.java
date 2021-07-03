package cn.cerc.mis.client;

public interface IServiceServer {

	String getToken();

	String getRequestUrl(String service);
}
