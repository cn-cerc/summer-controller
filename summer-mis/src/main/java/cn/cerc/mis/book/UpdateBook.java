package cn.cerc.mis.book;

import cn.cerc.db.core.ServiceException;

public interface UpdateBook extends IBook {

    // 在过帐时，需要区分年月
    boolean isKnowMonth();

    // 对登记到帐本的的数据进行更新
    void update() throws ServiceException;
}
