package cn.cerc.mis.ado;

public interface ISupplierFindBatch<T> {

    void load(BatchCache<T> findBatch);

}
