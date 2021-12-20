package cn.cerc.mis.ado;

import javax.persistence.Column;
import javax.persistence.Entity;

import cn.cerc.core.DataRow;
import cn.cerc.core.DataSet;
import cn.cerc.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.ado.EntityCache.VirtualEntityImpl;

@Entity
@EntityKey(fields = { "corpNo_", "enanble_" })
public class TableTest1Entity implements VirtualEntityImpl {
    @Column
    private String corpNo_;
    @Column
    private Boolean enanble_;
    @Column
    private Double amount_;

    @Override
    public boolean fillItem(IHandle handle, Object entity, DataRow headIn) {
        return false;
    }

    @Override
    public DataSet loadItems(IHandle handle, DataRow headIn) {
        return null;
    }
}
