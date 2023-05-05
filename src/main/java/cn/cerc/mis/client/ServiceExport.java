package cn.cerc.mis.client;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;

public class ServiceExport {

    public static String build(IHandle handle, DataSet dataIn) {
        if (dataIn == null)
            throw new RuntimeException("export dataIn can not be null.");
        String timestamp = String.valueOf(System.currentTimeMillis());
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.User.ExportKey, handle.getUserCode(), timestamp)) {
            buff.setValue("data", dataIn.json());
        }
        return timestamp;
    }

}
