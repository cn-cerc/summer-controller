package cn.cerc.mis.book;

import cn.cerc.db.core.Datetime;
import cn.cerc.db.core.Datetime.DateType;

public class VirtualData implements IBookData {
    private Datetime date;
    private IBook book;
    private IBookData bookData;

    public VirtualData(IBook book, IBookData bookData, int month) {
        this.book = book;
        this.bookData = bookData;
        this.date = bookData.getDate().inc(DateType.Month, month).toMonthBof();
    }

    public IBookData getBookData() {
        return bookData;
    }

    public IBook getBook() {
        return book;
    }

    @Override
    public Datetime getDate() {
        return date;
    }

    @Override
    public boolean check() {
        return true;
    }

    public boolean isOwner(IBook book) {
        return this.book == book;
    }
}
