package cn.cerc.mis.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Version;

import cn.cerc.db.core.DataException;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ServiceException;
import cn.cerc.mis.ado.CustomEntity;
import cn.cerc.mis.ado.EmptyEntity;
import cn.cerc.mis.log.JayunLogParser;

public abstract class CustomEntityService<HI extends CustomEntity, BI extends CustomEntity, HO extends CustomEntity, BO extends CustomEntity>
        implements IService {

    public DataSet execute(IHandle handle, DataSet dataIn) throws ServiceException, DataException {
        HI headIn = null;
        List<BI> bodyIn = null;
        // 检验数据单头与单身
        if (dataIn.head().fields().size() > 0) {
            var map = this.getMetaHeadIn();
            for (var field : map.keySet()) {
                var column = map.get(field);
                if (!column.nullable()) {
                    if (!dataIn.head().hasValue(field.getName()))
                        throw new DataValidateException(String.format("输入单头参数 %s 必须有值", field.getName()));
                }
            }
            headIn = dataIn.head().asEntity(getHeadInClass());
        }
        if (dataIn.size() > 0) {
            var map = this.getMetaBodyIn();
            bodyIn = new ArrayList<>();
            for (var row : dataIn) {
                for (var field : map.keySet()) {
                    var column = map.get(field);
                    if (!column.nullable()) {
                        if (!row.hasValue(field.getName()))
                            throw new DataValidateException(String.format("输入单身参数 %s 必须有值", field.getName()));
                    }
                }
                bodyIn.add(row.asEntity(getBodyInClass()));
            }
        }
        return this.call(handle, headIn, bodyIn);
    }

    public final DataSet call(IHandle handle, HI headIn, List<BI> bodyIn) throws ServiceException, DataException {
        if (headIn != null)
            validateHeadIn(headIn);
        if (bodyIn != null) {
            for (var body : bodyIn)
                validateBodyIn(body);
        }
        var dataOut = this.process(handle, headIn, bodyIn);
        if (dataOut.head().fields().size() > 0) {
            var map = this.getMetaHeadOut();
            for (var field : map.keySet()) {
                Column column = map.get(field);
                if (!column.nullable()) {
                    if (!dataOut.head().hasValue(field.getName()))
                        throw new DataValidateException(String.format("输出单头数据 %s 必须有值", field.getName()));
                }
            }
        }
        if (dataOut.size() > 0) {
            var map = this.getMetaBodyOut();
            for (var field : map.keySet()) {
                Column column = map.get(field);
                if (!column.nullable()) {
                    if (!dataOut.exists(field.getName()))
                        throw new DataValidateException(String.format("输出单身字段 %s 必须存在", field.getName()));
                }
            }
            var flag = false;
            for (var row : dataOut) {
                for (var field : map.keySet()) {
                    Column column = map.get(field);
                    if (!column.nullable()) {
                        if (row.getValue(field.getName()) == null) {
                            String message = String.format("%s 输出单身数据字段 %s 必须有值", this.getClass().getSimpleName(),
                                    field.getName());
                            RuntimeException exception = new RuntimeException(message);
                            JayunLogParser.warn(this.getClass(), exception);
                            flag = true;
                        }
                    }
                }
                if (flag)
                    break;
            }
        }
        return dataOut;
    }

    public final Map<Field, Column> getMetaHeadIn() {
        return getEntityMeta(this.getHeadInClass());
    }

    public final Map<Field, Column> getMetaBodyIn() {
        return getEntityMeta(this.getBodyInClass());
    }

    public final Map<Field, Column> getMetaHeadOut() {
        return getEntityMeta(this.getHeadOutClass());
    }

    public final Map<Field, Column> getMetaBodyOut() {
        return getEntityMeta(this.getBodyOutClass());
    }

    protected abstract DataSet process(IHandle handle, HI headIn, List<BI> bodyIn)
            throws ServiceException, DataException;

    /** 检验传入参数的 head 值 */
    protected void validateHeadIn(HI head) throws DataValidateException {

    }

    /** 检验传入参数的 body 值 */
    protected void validateBodyIn(BI body) throws DataValidateException {

    }

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

    private Map<Field, Column> getEntityMeta(Class<? extends CustomEntity> clazz) {
        var map = new LinkedHashMap<Field, Column>();
        if (clazz == EmptyEntity.class)
            return Map.of();
        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                // 开放读取权限
                if (field.getModifiers() == Modifier.PRIVATE || field.getModifiers() == Modifier.PROTECTED)
                    field.setAccessible(true);
                if (field.getAnnotation(Version.class) != null)
                    continue;
                if (field.getAnnotation(Id.class) != null)
                    continue;
                map.put(field, column);
            }
        }
        if (clazz.getSuperclass() != CustomEntity.class) {
            Class<?> superclass = clazz.getSuperclass();
            if (CustomEntity.class.isAssignableFrom(superclass)) {
                Class<? extends CustomEntity> subclass = superclass.asSubclass(CustomEntity.class);
                map.putAll(getEntityMeta(subclass));
            }
        }
        return map;
    }
}
