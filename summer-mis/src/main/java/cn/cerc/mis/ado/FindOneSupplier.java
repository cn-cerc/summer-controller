package cn.cerc.mis.ado;

public interface FindOneSupplier<T> {
    T get(Object... values);
}
