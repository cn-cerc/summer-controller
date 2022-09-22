package cn.cerc.mis.ado;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.BodyLogField;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityLog;
import cn.cerc.db.core.FieldDefs;
import cn.cerc.db.core.FieldMeta;
import cn.cerc.db.core.FieldMeta.FieldKind;
import cn.cerc.db.core.HeadLogField;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlQuery;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.core.Application;

public class EntityLogUtils {

    public enum AppendModelEnum {
        添加,
        修改
    }

    private static final Logger log = LoggerFactory.getLogger(EntityLogUtils.class);
    private IEntityLog entityLog = Application.getBean(IEntityLog.class);
    
    private final static String headTemplate = "%s：%s";
    private final static String bodyTemplate = "%s：%s->%s；";
    private final static String appendTemplate = "新增了 %s 主体信息为：%s";
    private final static String modifyTemplate = "%s 主体信息为：%s ；更改了：%s";

    private IHandle handle;
    private int type;
    private AppendModelEnum model = AppendModelEnum.添加;

    private String headStr;
    private String bodyStr;

    private DataRow oldData;
    private DataRow newData;
    private Class<?> classz;
    // 实体类名称
    private String entityName = "";

    private FieldDefs fields = new FieldDefs();

    private boolean openInsertLog;
    private boolean result = true;

    private EntityLogUtils() {

    }

    /**
     * 比较两个结果集，不同的值会记录下来
     * 
     * @param handle handle
     */
    public static EntityLogUtils create(IHandle handle, EntityImpl entity, AppendModelEnum model) {
        EntityLogUtils compareValueUtils = new EntityLogUtils();
        compareValueUtils.handle = handle;
        compareValueUtils.classz = entity.getClass();
        if (handle instanceof SqlQuery) {
            compareValueUtils.newData = new DataRow().loadFromEntity(entity);
            compareValueUtils.oldData = ((DataSet) handle).current();
        }
        compareValueUtils.model = model;
        compareValueUtils.init();
        return compareValueUtils;
    }

    private void init() {
        try {
            long startTime = System.currentTimeMillis();
            // 读取主体和审计字段
            readDefine();
            // 初始化实体类的注解信息
            initEntityAnnotation();
            // 初始化主体字段
            initHeadField();
            // 初始化审计字段
            if (model == AppendModelEnum.修改)
                initBodyField();
            System.err.println(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            this.result = false;
            log.error("字段初始化错误：" + e.getMessage());
        }
    }

    public boolean execute() {
        if (openInsertLog && result && entityLog != null) {
            if (model == AppendModelEnum.添加)
                entityLog.run(handle, type, String.format(appendTemplate, entityName, headStr));
            else
                entityLog.run(handle, type, String.format(modifyTemplate, entityName, headStr, bodyStr));
        } else {
            return false;
        }
        return true;
    }

    /**
     * 初始化头部信息
     * 
     */
    private void initHeadField() {
        List<String> list = new LinkedList<>();
        for (FieldMeta filed : this.fields.getHeadFields()) {
            String name = this.fields.get(filed.code()).name();
            String value = this.newData.getString(filed.code());
            value = Utils.isEmpty(value) ? "(空)" : value;
            if (name != null)
                list.add(String.format(headTemplate, name, value));
        }
        this.headStr = list.size() > 0 ? StringUtils.join(list, ",") : "（空）";
    }

    /**
     * 将身体字段比较记录改变的值
     * 
     */
    public void initBodyField() throws NoSuchFieldException, SecurityException {
        StringBuffer msg = new StringBuffer();
        for (FieldMeta fieldMeta : this.fields.getBodyFields()) {
            String name = fieldMeta.name();
            String code = fieldMeta.code();
            Field field = this.classz.getDeclaredField(code);
            if (name != null && code != null) {
                String tempMsg = "";
                if (field.getType().isEnum()) {
                    Enum<?> newValue = (Enum<?>) field.getType().getEnumConstants()[this.newData.getInt(code)];
                    Enum<?> oldValue = (Enum<?>) field.getType().getEnumConstants()[this.oldData.getInt(code)];
                    if (newValue != oldValue)
                        tempMsg = String.format(bodyTemplate, name, oldValue.name(), newValue.name());
                } else {
                    String newValue = this.newData.getString(code);
                    String oldValue = this.oldData.getString(code);
                    if (this.newData.getValue(code) instanceof Boolean)
                        newValue = newData.getBoolean(code) ? "是" : "否";
                    if (this.oldData.getValue(code) instanceof Boolean)
                        oldValue = this.oldData.getBoolean(code) ? "是" : "否";
                    if (!newValue.equals(oldValue)) {
                        oldValue = Utils.isEmpty(oldValue) ? "(空)" : oldValue;
                        newValue = Utils.isEmpty(newValue) ? "(空)" : newValue;
                        tempMsg = String.format(bodyTemplate, name, oldValue, newValue);
                    }
                }
                msg.append(tempMsg);
            }
        }
        this.bodyStr = Utils.isEmpty(msg.toString()) ? "（空）" : msg.toString();
    }

    private void initEntityAnnotation() {
        EntityLog entityLog = this.classz.getAnnotation(EntityLog.class);
        if (entityLog != null) {
            this.type = entityLog.historyType();
            this.openInsertLog = true;
            this.entityLog = (IEntityLog) Application.getBean("entityLogImpl" + type);
        }
        Describe describe = this.classz.getAnnotation(Describe.class);
        if (describe != null)
            this.entityName += describe.name();
    }

    private void readDefine() {
        Field[] fields = this.classz.getDeclaredFields();
        List<String> list = new ArrayList<>();
        for (Field field : fields) {
            HeadLogField headLogField = field.getDeclaredAnnotation(HeadLogField.class);
            BodyLogField bodyLogField = field.getDeclaredAnnotation(BodyLogField.class);
            if (headLogField != null || bodyLogField != null) {
                list.add(field.getName());
                this.fields.add(field.getName(), FieldKind.Storage);
            }
        }
        this.fields.readDefine(classz, list.toArray(new String[0]));
    }
}
