package cn.cerc.mis.core;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Description;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.EntityHelper;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.ado.CustomEntity;
import cn.cerc.mis.ado.EmptyEntity;

public abstract class CustomEntityService<HI extends CustomEntity, BI extends CustomEntity, HO extends CustomEntity, BO extends CustomEntity>
        implements IService {

    public final DataSet execute(IHandle handle, DataSet dataIn) throws DataValidateException {
        HI headIn = null;
        List<BI> bodyIn = null;
        // 检验数据单头与单身
        if (dataIn.fields().size() > 0)
            headIn = dataIn.head().asEntity(getHeadInClass());
        if (dataIn.size() > 0) {
            bodyIn = new ArrayList<BI>();
            for (var row : dataIn)
                bodyIn.add(row.asEntity(getBodyInClass()));
        }
        return this.call(handle, headIn, bodyIn);
    }

    public final DataSet call(IHandle handle, HI headIn, List<BI> bodyIn) throws DataValidateException {
        if (headIn != null)
            validateHeadIn(headIn);
        if (bodyIn != null) {
            for (var body : bodyIn)
                validateBodyIn(body);
        }
        return this.process(handle, headIn, bodyIn);
    }

    public final DataSet getMetaHeadIn(IHandle handle, DataSet dataIn) {
        return getEntityMeta(this.getHeadInClass());
    }

    public final DataSet getMetaBodyIn(IHandle handle, DataSet dataIn) {
        return getEntityMeta(this.getBodyInClass());
    }

    public final DataSet getMetaHeadOut(IHandle handle, DataSet dataIn) {
        return getEntityMeta(this.getHeadOutClass());
    }

    public final DataSet getMetaBodyOut(IHandle handle, DataSet dataIn) {
        return getEntityMeta(this.getBodyOutClass());
    }

    protected abstract DataSet process(IHandle handle, HI headIn, List<BI> bodyIn);

    /** 检验传入参数的 head 值 */
    protected abstract void validateHeadIn(HI head) throws DataValidateException;

    /** 检验传入参数的 body 值 */
    protected abstract void validateBodyIn(BI body) throws DataValidateException;

    @SuppressWarnings("unchecked")
    protected final Class<HI> getHeadInClass() {
        return (Class<HI>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    protected final Class<BI> getBodyInClass() {
        return (Class<BI>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    @SuppressWarnings("unchecked")
    protected final Class<HO> getHeadOutClass() {
        return (Class<HO>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }

    @SuppressWarnings("unchecked")
    protected final Class<BO> getBodyOutClass() {
        return (Class<BO>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[3];
    }

    private DataSet getEntityMeta(Class<? extends CustomEntity> clazz) {
        DataSet dataOut = new DataSet();
        if (clazz == null)
            return dataOut;
        if (clazz == EmptyEntity.class)
            return dataOut;
        var helper = EntityHelper.create(clazz);
        for (var code : helper.fields().keySet()) {
            var field = helper.fields().get(code);
            dataOut.append();
            dataOut.setValue("code", code);
            dataOut.setValue("name", field.getName());
            var desc = field.getAnnotation(Description.class);
            if (desc != null)
                dataOut.setValue("name", desc.value());
            var desc2 = field.getAnnotation(Describe.class);
            if (desc2 != null) {
                dataOut.setValue("name", desc2.name());
                dataOut.setValue("remark", desc2.remark());
                dataOut.setValue("width", desc2.width());
            }
        }
        return dataOut;
    }
}
