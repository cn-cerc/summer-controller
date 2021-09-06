package cn.cerc.mis.tools;

import cn.cerc.core.Datetime;

public class DurationSection {
    private Datetime dateFrom;
    private Datetime dateTo;

    public DurationSection(Datetime dateFrom, Datetime dateTo) {
        super();
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public Datetime getDateFrom() {
        dateFrom.setOptions(Datetime.yyyyMMdd_HHmmss);
        return dateFrom;
    }

    public void setDateFrom(Datetime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Datetime getDateTo() {
        dateTo.setOptions(Datetime.yyyyMMdd_HHmmss);
        return dateTo;
    }

    public void setDateTo(Datetime dateTo) {
        this.dateTo = dateTo;
    }

    public String getMonthFrom() {
        return dateFrom.getYearMonth();
    }
}
