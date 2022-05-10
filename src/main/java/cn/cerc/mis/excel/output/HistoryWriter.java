package cn.cerc.mis.excel.output;

import cn.cerc.db.core.IHandle;

public interface HistoryWriter {

    void start(IHandle handle, ExcelTemplate template);

    void finish(IHandle handle, ExcelTemplate template);

}
