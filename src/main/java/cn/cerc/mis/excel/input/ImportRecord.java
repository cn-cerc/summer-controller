package cn.cerc.mis.excel.input;

import cn.cerc.db.core.DataRow;

public interface ImportRecord {
    boolean process(DataRow rs) throws Exception;
}
