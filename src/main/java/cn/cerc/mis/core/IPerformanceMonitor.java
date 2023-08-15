package cn.cerc.mis.core;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface IPerformanceMonitor {

    void writeFormExecuteTime(IForm iForm, String funcCode, long startTime);

    void writeServiceExecuteTime(IHandle handle, IService service, DataSet dataIn, String funcCode, long startTime);

}
