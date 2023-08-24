package cn.cerc.mis.ado;

import java.util.Optional;

@Deprecated
public class FindOneBatch<T> extends BatchCache<T> {

    public FindOneBatch(ISupplierFindOne<Optional<T>> findOne) {
        super(findOne);
    }
    
}
