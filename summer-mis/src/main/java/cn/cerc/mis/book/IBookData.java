package cn.cerc.mis.book;

import cn.cerc.core.Datetime;

public interface IBookData {
    Datetime getDate();

    boolean check();
}
