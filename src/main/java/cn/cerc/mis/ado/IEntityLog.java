package cn.cerc.mis.ado;

import cn.cerc.db.core.IHandle;

public interface IEntityLog {
    void run(IHandle handle, int historyType, String content);
}
