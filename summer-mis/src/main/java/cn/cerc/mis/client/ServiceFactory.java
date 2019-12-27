package cn.cerc.mis.client;

import cn.cerc.core.IHandle;
import cn.cerc.db.cache.Buffer;
import cn.cerc.db.mysql.MysqlConnection;
import cn.cerc.mis.core.BookHandle;
import cn.cerc.mis.core.ISystemTable;
import cn.cerc.mis.core.LocalService;

public class ServiceFactory {

    public static IServiceProxy get(IHandle handle, String corpNo) {
        if (corpNo.equals(handle.getCorpNo())) {
            return new LocalService(handle);
        } else {
            MysqlConnection curConn = (MysqlConnection) handle.getProperty(MysqlConnection.sessionId);
            String curDB = curConn.getDatabase();

            Buffer buff = new Buffer(ServiceFactory.class.getName(), corpNo);
            String tarDB = buff.getString("database");
            if ("".equals(tarDB) || null == tarDB) {
                RemoteService svr = new RemoteService(handle, ISystemTable.Public, "ApiDB.getDatabase");
                if (!svr.exec("bookNo", corpNo)) {
                    throw new RuntimeException(svr.getMessage());
                }
                tarDB = svr.getDataOut().getHead().getString("database");
                buff.setField("database", tarDB);
                buff.post();
            }

            if (curDB.equals(tarDB)) {
                return new LocalService(new BookHandle(handle, corpNo));
            } else {
                return new RemoteService(handle, corpNo);
            }
        }
    }

    public static IServiceProxy get(IHandle handle, String corpNo, String service) {
        IServiceProxy svr = ServiceFactory.get(handle, corpNo);
        svr.setService(service);
        return svr;
    }

}
