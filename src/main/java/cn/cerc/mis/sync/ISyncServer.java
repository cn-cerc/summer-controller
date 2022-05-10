package cn.cerc.mis.sync;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.ISession;

public interface ISyncServer {

    void push(ISession session, DataRow record);

    void repush(ISession session, DataRow record);

    int pop(ISession session, IPopProcesser popProcesser, int maxRecords);

}
