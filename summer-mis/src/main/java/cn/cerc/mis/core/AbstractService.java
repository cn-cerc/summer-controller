package cn.cerc.mis.core;

import org.springframework.beans.factory.annotation.Autowired;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Handle;

//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractService extends Handle implements IService {
    @Autowired
    public ISystemTable systemTable;

    public DataSet fail(String format, Object... args) {
        DataSet dataSet = new DataSet();
        if (args.length > 0) {
            dataSet.setMessage(String.format(format, args));
        } else {
            dataSet.setMessage(format);
        }
        return dataSet;
    }

}
