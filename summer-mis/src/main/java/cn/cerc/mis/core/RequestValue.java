package cn.cerc.mis.core;

import javax.servlet.http.HttpServletRequest;

import cn.cerc.core.Datetime;
import cn.cerc.core.FastDate;
import cn.cerc.core.FastTime;
import cn.cerc.core.KeyValue;

/**
 * 此类主要配合DataRow.setValueNotNull使用
 * 
 * @author ZhangGong 1416960@qq.com
 *
 */
public class RequestValue {
    private HttpServletRequest request = null;

    public RequestValue(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * 
     * @param field 字段代码
     * @return 判断是否存在（值可能为空）
     */
    public boolean exists(String field) {
        return request.getParameter(field) != null;
    }

    /**
     * 
     * @param field 字段代码
     * @return 判断是否存在且值是否为空
     */
    public boolean has(String field) {
        String val = request.getParameter(field);
        if (val == null)
            return false;
        return !"".equals(val);
    }

    public String getString(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asString() : null;
    }

    public Boolean getBoolean(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asBoolean() : null;
    }

    public Integer getInteger(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asInt() : null;
    }

    public Long getLong(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asLong() : null;
    }

    public Float getFloat(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asFloat() : null;
    }

    public Double getDouble(String field) {
        return has(field) ? new KeyValue(request.getParameter(field)).asDouble() : null;
    }

    public Datetime getDatetime(String field) {
        if (!has(field))
            return null;
        Datetime value = new KeyValue(request.getParameter(field)).asDatetime();
        return value.isEmpty() ? null : value;
    }

    public FastDate getFastDate(String field) {
        if (!has(field))
            return null;
        FastDate value = new KeyValue(request.getParameter(field)).asFastDate();
        return value.isEmpty() ? null : value;
    }

    public FastTime getFastTime(String field) {
        if (!has(field))
            return null;
        FastTime value = new KeyValue(request.getParameter(field)).asFastTime();
        return value.isEmpty() ? null : value;
    }
}
