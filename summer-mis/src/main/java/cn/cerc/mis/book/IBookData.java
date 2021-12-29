package cn.cerc.mis.book;

import cn.cerc.db.core.Datetime;

public interface IBookData {
    Datetime getDate();

    boolean check();
}
