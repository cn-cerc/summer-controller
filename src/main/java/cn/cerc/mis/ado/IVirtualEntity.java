package cn.cerc.mis.ado;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;

public interface IVirtualEntity<T> {
    /**
     * @param handle IHandle
     * @param entity Entity Object
     * @param headIn 要赋值的内容
     * @return 直接给 entity 赋值 values 是否成功
     */
    default boolean fillItem(IHandle handle, T entity, DataRow headIn) {
        return false;
    }

    /**
     * 先调用fillEntity，在其返回false时，再调用此函数
     * 
     * @param handle IHandle
     * @param headIn headIn 标识字段的值
     * @return 返回载入的数据，允许返回null
     */
    DataSet loadItems(IHandle handle, DataRow headIn);
}
