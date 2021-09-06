package cn.cerc.mis.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.servlet.http.HttpServletRequest;

import cn.cerc.core.IRecord;
import cn.cerc.core.TDate;
import cn.cerc.core.TDateTime;

public class RequestRecord {
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

    public String getString(String field) {
        return req.getParameter(field);
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

    public int getInt(String field) {
        return Integer.parseInt(req.getParameter(field));
    }

    public BigInteger getBigInteger(String field) {
        return new BigInteger(req.getParameter(field));
    }

    public BigDecimal getBigDecimal(String field) {
        return new BigDecimal(req.getParameter(field));
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

    public double getDouble(String field) {
        return Double.parseDouble(req.getParameter(field));
    }

    public boolean hasBoolean(String field) {
        String val = req.getParameter(field);
        if (val == null) {
            return false;
        }
        return !"".equals(val);
    }

    public boolean getBoolean(String field) {
        return "true".equals(req.getParameter(field));
    }

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

    public TDateTime getDateTime(String field) {
        String value = req.getParameter(field);
        if (value != null) {
            return new TDateTime(value);
        } else {
            return null;
        }
    }

    public IRecord setField(String field, Object value) {
        req.setAttribute(field, value);
        return null;
    }

    public boolean exists(String field) {
        return req.getParameter(field) != null;
    }

    public Object getField(String field) {
        return req.getParameter(field);
    }

}
