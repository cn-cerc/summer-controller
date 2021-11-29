package cn.cerc.mis.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.ClassResource;
import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
import cn.cerc.core.ISession;
import cn.cerc.core.Utils;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.core.LocalService;
import cn.cerc.mis.core.ServiceState;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;

public class RemoteService extends Handle implements IServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(RemoteService.class);
    private static final ClassResource res = new ClassResource(RemoteService.class, SummerMIS.ID);
    private IServiceServer server;
    private String service;
    private DataSet dataIn;
    private DataSet dataOut;

    public RemoteService(IHandle handle) {
        super(handle);
    }

    @Override
    public boolean exec(Object... args) {
        if (args.length > 0) {
            DataRow headIn = getDataIn().head();
            if (args.length % 2 != 0) {
                throw new RuntimeException(res.getString(1, "传入的参数数量必须为偶数！"));
            }
            for (int i = 0; i < args.length; i = i + 2) {
                headIn.setValue(args[i].toString(), args[i + 1]);
            }
        }

        // 若未定义远程主机，则改为执行本地服务
        if (this.server == null || this.server.getRequestUrl(this, service) == null) {
            LocalService svr = new LocalService(this);
            svr.setService(this.getService());
            svr.setDataIn(getDataIn());
            svr.exec();
            this.setDataOut(svr.getDataOut());
            return getDataOut().state() > ServiceState.ERROR;
        }

        log.debug(this.service);
        if (Utils.isEmpty(this.service)) {
            this.setMessage(res.getString(2, "服务代码不允许为空"));
            return false;
        }

        String url = server.getRequestUrl(this, this.getService());
        try {
            Curl curl = new Curl();
            curl.put("dataIn", getDataIn().toJson());
            if (this.server != null && server.getToken(this) != null)
                curl.put(ISession.TOKEN, this.server.getToken(this));
            log.debug("request: {}", url);

            String response = null;
            try {
                log.debug("post: {}", curl.getParameters());
                response = curl.doPost(url);
                log.debug("response: {}", response);
            } catch (IOException e) {
                getDataOut().setState(ServiceState.CALL_TIMEOUT).setMessage(res.getString(5, "远程服务异常"));
                return false;
            }
            this.getDataOut().setJson(response);

            return getDataOut().state() > ServiceState.ERROR;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e.getCause() != null) {
                setMessage(e.getCause().getMessage());
            } else {
                setMessage(e.getMessage());
            }
            return false;
        }
    }

    @Override
    public final String getService() {
        return service;
    }

    @Override
    public final RemoteService setService(String service) {
        this.service = service;
        return this;
    }

    @Override
    public final String getMessage() {
        return getDataOut().getMessage();
    }

    public final void setMessage(String message) {
        getDataOut().setMessage(message);
    }

    @Override
    public final DataSet getDataOut() {
        if (dataOut == null)
            dataOut = new DataSet();
        return dataOut;
    }

    protected void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

    @Override
    public final DataSet getDataIn() {
        if (dataIn == null)
            dataIn = new DataSet();
        return dataIn;
    }

    public void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

    @Deprecated
    public String getExportKey() {
        String tmp = "" + System.currentTimeMillis();
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, this.getUserCode(), tmp)) {
            buff.setValue("data", this.getDataIn().toJson());
        }
        return tmp;
    }

    public IServiceServer getServer() {
        return server;
    }

    public RemoteService setServer(IServiceServer server) {
        this.server = server;
        return this;
    }

}
