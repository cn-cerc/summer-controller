package cn.cerc.mis.sync;

import cn.cerc.core.DataRow;
import cn.cerc.db.core.IHandle;

public interface IPushProcesser extends IHandle {

    boolean appendRecord(DataRow record);

    boolean deleteRecord(DataRow record);

    boolean updateRecord(DataRow record);

    boolean resetRecord(DataRow record);

    void abortRecord(DataRow record, SyncOpera opera);

}
