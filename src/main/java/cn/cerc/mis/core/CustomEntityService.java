package cn.cerc.mis.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Version;

import cn.cerc.db.core.ClassData;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.mis.ado.CustomEntity;
import cn.cerc.mis.ado.EmptyEntity;

public abstract class CustomEntityService<HI extends CustomEntity, BI extends CustomEntity, HO extends CustomEntity, BO extends CustomEntity>
        implements IService {

    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException {
        HI headIn = null;
        List<BI> bodyIn = null;
        // 检验数据单头与单身
        if (dataIn.head().fields().size() > 0)
            headIn = dataIn.head().asEntity(getHeadInClass());
        if (dataIn.size() > 0) {
            bodyIn = new ArrayList<BI>();
            for (var row : dataIn)
                bodyIn.add(row.asEntity(getBodyInClass()));
        }
        return this.call(handle, headIn, bodyIn);
    }

    public final DataSet call(IHandle handle, HI headIn, List<BI> bodyIn) throws ServiceException {
        if (headIn != null)
            validateHeadIn(headIn);
        if (bodyIn != null) {
            for (var body : bodyIn)
                validateBodyIn(body);
        }
        return this.process(handle, headIn, bodyIn);
    }

    public final List<Field> getMetaHeadIn() {
        return getEntityMeta(this.getHeadInClass());
    }

    public final List<Field> getMetaBodyIn() {
        return getEntityMeta(this.getBodyInClass());
    }

    public final List<Field> getMetaHeadOut() {
        return getEntityMeta(this.getHeadOutClass());
    }

    public final List<Field> getMetaBodyOut() {
        return getEntityMeta(this.getBodyOutClass());
    }

    protected abstract DataSet process(IHandle handle, HI headIn, List<BI> bodyIn) throws ServiceException;

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

    private List<Field> getEntityMeta(Class<? extends CustomEntity> clazz) {
        var list = new ArrayList<Field>();
        if (clazz == EmptyEntity.class)
            return list;
        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            // 开放读取权限
            if (field.getModifiers() == ClassData.DEFAULT || field.getModifiers() == ClassData.PRIVATE
                    || field.getModifiers() == ClassData.PROTECTED)
                field.setAccessible(true);
            if (field.getAnnotation(Version.class) != null)
                continue;
            if (field.getAnnotation(Id.class) != null)
                continue;
            list.add(field);
        }
        return list;
    }
}
