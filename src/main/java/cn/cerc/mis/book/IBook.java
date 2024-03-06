package cn.cerc.mis.book;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.ServiceException;

public interface IBook extends IBookEnroll {
    // 初始化（仅调用一次）
    void init(IBookManage manage);

    // 初始化（ 会被每月调用）
    void ready() throws ServiceException;

    // 将帐本的更新，保存到数据库中
    void save() throws ServiceException, DataException;
}
