package cn.cerc.mis.sync;

import cn.cerc.core.ISession;
import cn.cerc.core.DataRow;

public interface IPopProcesser {

    boolean popRecord(ISession session, DataRow record, boolean isQueue);

}
