package cn.cerc.mis.core;

import javax.servlet.http.HttpServletRequest;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
import cn.cerc.core.Datetime.DateType;
import cn.cerc.core.ISession;
import cn.cerc.core.MD5;
import cn.cerc.db.core.Handle;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.other.MemoryBuffer;

public class SegmentQuery extends Handle {
    private DataSet dataIn;
    private DataSet dataOut;

    public SegmentQuery(CustomService owner) {
        super(owner);
        this.dataIn = owner.getDataIn();
        this.dataOut = owner.getDataOut();
    }

    public SegmentQuery(IHandle handle, DataSet dataIn, DataSet dataOut) {
        super(handle);
        this.dataIn = dataIn;
        this.dataOut = dataOut;
    }

    public boolean enable(String fromField, String toField) {
        return enable(fromField, toField, DateType.Month);
    }

    public boolean enable(String fromField, String toField, DateType dateType) {
        DataRow headIn = dataIn.getHead();
        if (!headIn.getBoolean("segmentQuery"))
            return false;

        HttpServletRequest request = (HttpServletRequest) this.getSession().getProperty(ISession.REQUEST);
        String sessionId = request.getSession().getId();
        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Service.BigData, this.getClass().getName(), sessionId,
                MD5.get(dataIn.toJson()))) {
            if (buff.isNull()) {
                buff.setValue("beginDate", headIn.getDatetime(fromField));
                buff.setValue("endDate", headIn.getDatetime(toField).toDayEnd());
                buff.setValue("curBegin", headIn.getDatetime(fromField));
                buff.setValue("curEnd", headIn.getDatetime(fromField).toDayEnd());
                headIn.setValue(fromField, buff.getDatetime("beginDate"));
                headIn.setValue(toField, headIn.getDatetime(fromField).inc(dateType, 1).toDayEnd());
            } else {
                headIn.setValue(fromField, buff.getDatetime("curEnd").inc(DateType.Day, 1).toDayStart());
                headIn.setValue(toField, buff.getDatetime("curEnd").inc(dateType, 1).toDayEnd());
            }

            if (headIn.getDatetime(toField).compareTo(buff.getDatetime("endDate")) > 0) {
                headIn.setValue(toField, buff.getDatetime("endDate"));
                buff.clear();
            } else {
                buff.setValue("curBegin", headIn.getDatetime(fromField));
                buff.setValue("curEnd", headIn.getDatetime(toField));
                buff.post();
                dataOut.getHead().setValue("_has_next_", true);
            }
        }
        return true;
    }
}
