package cn.cerc.mis.sync;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.ISession;

public interface IPopProcesser {

    boolean popRecord(ISession session, DataRow record, boolean isQueue);

}
