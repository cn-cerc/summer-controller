package cn.cerc.mis.tools;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

import cn.cerc.core.Datetime;
import cn.cerc.core.Datetime.DateType;

public class DurationSplit implements Iterable<DurationSection>, Iterator<DurationSection> {
    private Datetime beginDate;
    private Datetime endDate;
    private Datetime dateFrom;
    private Datetime dateTo;
    private int total;

    public DurationSplit(Datetime beginDate, Datetime endDate) {
        this.beginDate = beginDate;
        this.endDate = endDate;
        if (beginDate == null) {
            throw new RuntimeException("beginDate is null");
        }
    }

    public static void main(String[] args) throws ParseException {
        DurationSplit duration = new DurationSplit(new Datetime("2016-07-01"), new Datetime("201609"));
        for (DurationSection section : duration) {
            System.out.println(String.format("beginDate: %s, endDate: %s", section.getDateFrom(), section.getDateTo()));
        }
    }

    public Datetime getDateFrom() {
        return dateFrom;
    }

    public Datetime getDateTo() {
        return dateTo;
    }

    public Datetime getBeginDate() {
        return beginDate;
    }

    public Datetime getEndDate() {
        return endDate;
    }

    @Override
    public Iterator<DurationSection> iterator() {
        dateFrom = beginDate;
        dateTo = beginDate.toMonthEof();
        total = -1;
        return this;
    }

    @Override
    public boolean hasNext() {
        if (++total == 0) {
            return beginDate.before(endDate);
        }

        dateFrom = dateTo.clone().inc(DateType.Month, 1).toMonthBof();
        return endDate.after(dateTo);
    }

    @Override
    public DurationSection next() {
        if (total == 0) {
            dateFrom = beginDate;
            dateTo = beginDate.toMonthEof();
        } else {
            dateFrom = dateTo.inc(DateType.Month, 1).toMonthBof();
            dateTo = dateFrom.toMonthEof();
        }
        if (dateTo.subtract(DateType.Day, endDate) > 0)
            dateTo = endDate;
        if ("00:00:00".equals(dateTo.getTime())) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateTo.inc(DateType.Day, 1).asBaseDate());
            cal.set(Calendar.MILLISECOND, cal.get(Calendar.MILLISECOND) - 1);
            dateTo.setTimestamp(cal.getTime().getTime());
        }
        return new DurationSection(dateFrom, dateTo);
    }
}
