package cn.cerc.mis.tools;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

import cn.cerc.core.Datetime;
import cn.cerc.core.Datetime.DateType;
import cn.cerc.core.TDateTime;

public class DurationSplit implements Iterable<DurationSection>, Iterator<DurationSection> {
    private TDateTime beginDate;
    private TDateTime endDate;
    private TDateTime dateFrom;
    private TDateTime dateTo;
    private int total;

    public DurationSplit(Datetime beginDate, Datetime endDate) {
        this.beginDate = new TDateTime(beginDate.asBaseDate());
        this.endDate = new TDateTime(endDate.asBaseDate());
        if (beginDate == null) {
            throw new RuntimeException("beginDate is null");
        }
    }

    public static void main(String[] args) throws ParseException {
        DurationSplit duration = new DurationSplit(TDateTime.StrToDate("2016-07-01"), TDateTime.StrToDate("201609"));
        for (DurationSection section : duration) {
            System.out.println(String.format("beginDate: %s, endDate: %s", section.getDateFrom(), section.getDateTo()));
        }
    }

    public TDateTime getDateFrom() {
        return dateFrom;
    }

    public TDateTime getDateTo() {
        return dateTo;
    }

    public TDateTime getBeginDate() {
        return beginDate;
    }

    public TDateTime getEndDate() {
        return endDate;
    }

    @Override
    public Iterator<DurationSection> iterator() {
        dateFrom = beginDate;
        dateTo = beginDate.monthEof();
        total = -1;
        return this;
    }

    @Override
    public boolean hasNext() {
        if (++total == 0) {
            return beginDate.before(endDate);
        }

        dateFrom = dateTo.incMonth(1).monthBof();
        return endDate.after(dateTo);
    }

    @Override
    public DurationSection next() {
        if (total == 0) {
            dateFrom = beginDate;
            dateTo = beginDate.monthEof();
        } else {
            dateFrom = dateTo.incMonth(1).monthBof();
            dateTo = dateFrom.monthEof();
        }
        if (dateTo.subtract(DateType.Day, endDate) > 0) {
            dateTo = endDate;
        }
        if ("00:00:00".equals(dateTo.getTime())) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateTo.incDay(1).asBaseDate());
            cal.set(Calendar.MILLISECOND, cal.get(Calendar.MILLISECOND) - 1);
            dateTo.setTimestamp(cal.getTime().getTime());
        }
        return new DurationSection(dateFrom, dateTo);
    }
}
