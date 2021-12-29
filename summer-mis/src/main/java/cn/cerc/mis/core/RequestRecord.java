package cn.cerc.mis.core;

import javax.servlet.http.HttpServletRequest;

import cn.cerc.db.core.IRecord;
import cn.cerc.db.core.TDate;
import cn.cerc.db.core.TDateTime;

@Deprecated
public class RequestRecord implements IRecord {
    private HttpServletRequest req = null;

    public RequestRecord(HttpServletRequest req) {
        this.req = req;
    }

    public boolean hasString(String field) {
        String val = req.getParameter(field);
        if (val == null) {
            return false;
        }
        return !"".equals(val);
    }

    public boolean hasInt(String field) {
        String val = req.getParameter(field);
        if (val == null) {
            return false;
        }
        if ("".equals(val)) {
            return false;
        }
        for (int i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            if (!(ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7'
                    || ch == '8' || ch == '9')) {
                return false;
            }
        }
        return true;
    }

    public boolean hasDouble(String field) {
        String val = req.getParameter(field);
        if (val == null) {
            return false;
        }
        if ("".equals(val)) {
            return false;
        }
        for (int i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            if (!(ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7'
                    || ch == '8' || ch == '9' || ch == '.')) {
                return false;
            }
        }
        return true;
    }

    public boolean hasBoolean(String field) {
        String val = req.getParameter(field);
        if (val == null) {
            return false;
        }
        return !"".equals(val);
    }

    public boolean hasDatetime(String field) {
        return !getDatetime(field).isEmpty();
    }

    @Deprecated
    public boolean hasDateTime(String field) {
        TDateTime dt = new TDateTime(req.getParameter(field));
        return !dt.isEmpty();
    }

    @Deprecated
    public TDate getDate(String field) {
        TDateTime result = this.getDateTime(field);
        if (result != null) {
            return result.asDate();
        } else {
            return null;
        }
    }

    @Deprecated
    public TDateTime getDateTime(String field) {
        String value = req.getParameter(field);
        if (value != null) {
            return new TDateTime(value);
        } else {
            return null;
        }
    }

    @Deprecated
    public IRecord setField(String field, Object value) {
        req.setAttribute(field, value);
        return null;
    }

    @Override
    public boolean exists(String field) {
        return req.getParameter(field) != null;
    }

    @Deprecated
    public Object getField(String field) {
        return getValue(field);
    }

    @Override
    public Object setValue(String field, Object value) {
        throw new RuntimeException("not support method: setValue");
    }

    @Override
    public String getValue(String field) {
        return req.getParameter(field);
    }

}
