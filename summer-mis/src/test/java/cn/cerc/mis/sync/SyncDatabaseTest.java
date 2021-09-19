package cn.cerc.mis.sync;

import cn.cerc.core.DataRow;
import cn.cerc.mis.custom.SessionDefault;

public class SyncDatabaseTest {

    public static void main(String[] args) {
        DataRow record = new DataRow();
        record.setField("code", "a01");
        SyncDatabase db = new SyncDatabase(new SyncQueueTest());
        db.push(new SessionDefault(), "part", record, SyncOpera.Update);
    }

}
