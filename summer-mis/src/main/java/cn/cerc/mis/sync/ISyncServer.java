package cn.cerc.mis.sync;

import cn.cerc.core.ISession;
import cn.cerc.core.DataRow;

public interface ISyncServer {

    void push(ISession session, DataRow record);

    void repush(ISession session, DataRow record);

    int pop(ISession session, IPopProcesser popProcesser, int maxRecords);

}
