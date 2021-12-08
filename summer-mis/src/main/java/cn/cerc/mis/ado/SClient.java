package cn.cerc.mis.ado;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
import cn.cerc.core.SqlText;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.other.SqlFieldFilter;
import cn.cerc.db.other.SqlTextDecode;
import cn.cerc.mis.core.LocalService;

public class SClient extends DataSet {
    private static final long serialVersionUID = -164270920329432676L;
    private String service = "";
    private IHandle handle;

    public SClient(IHandle handle) {
        super();
        this.setBatchSave(true);
        this.handle = handle;
    }

    public void setService(String service) {
        this.service = service;
    }

    private LocalService createService() {
        DataRow headIn = this.head();
        SqlTextDecode decode = new SqlTextDecode(this.service);
        for (SqlFieldFilter item : decode.getWhere())
            headIn.setValue(item.getField(), item.getValue());

        String cmd = this.service;
        if (service.toUpperCase().contains(" FROM ")) {
            headIn.setValue("_sql_", service);
            cmd = SqlText.findTableName(this.service);
        }
        LocalService svr = new LocalService(this.handle, cmd);
        svr.dataIn().setJson(this.json());
        return svr;
    }

    public void open() {
        LocalService svr = createService();
        svr.exec();
        this.setJson(svr.dataOut().json());
        this.mergeChangeLog();
    }

    public void save() {
        this.setCrud(true);
        try {
            open();
        } finally {
            this.setCrud(false);
        }
    }

}
