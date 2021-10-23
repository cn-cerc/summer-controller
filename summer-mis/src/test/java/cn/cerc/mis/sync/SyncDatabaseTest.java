package cn.cerc.mis.sync;

import cn.cerc.core.DataRow;
import cn.cerc.mis.security.SessionDefault;

public class SyncDatabaseTest {

    public static void main(String[] args) {
        DataRow record = new DataRow();
        record.setValue("code", "a01");
        SyncDatabase db = new SyncDatabase(new SyncQueueTest());
        db.push(new SessionDefault(), "part", record, SyncOpera.Update);
    }

}
