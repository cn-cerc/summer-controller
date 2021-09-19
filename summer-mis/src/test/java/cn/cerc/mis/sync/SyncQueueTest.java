package cn.cerc.mis.sync;

import cn.cerc.core.ISession;
import cn.cerc.core.DataRow;

public class SyncQueueTest implements ISyncServer {

    @Override
    public void push(ISession session, DataRow record) {
        System.out.println("push:" + record);
    }

    @Override
    public void repush(ISession session, DataRow record) {
        System.out.println("repush:" + record);
    }

    @Override
    public int pop(ISession session, IPopProcesser processer, int maxRecords) {
        System.err.println("not yet pop");
        return 0;
    }

}
