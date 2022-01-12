package cn.cerc.mis.book;

import cn.cerc.db.core.ServiceException;

public interface IResetBook extends IBook {
    // 对登记到帐本的的数据进行重置（回算）
    void reset() throws ServiceException;
}
