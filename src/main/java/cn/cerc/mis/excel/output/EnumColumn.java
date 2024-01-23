package cn.cerc.mis.excel.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 仅支持使用枚举下标储存的数据
 * <p>
 * 使导出Excel文件支持枚举类型 在导出模板中加上
 * <p>
 * class:
 * <p>
 * 独立枚举请使用 package.ClassName
 * <p>
 * 实体内部枚举使用 package.ClassName$InnerClass
 */
public class EnumColumn extends Column {
    private static final Logger log = LoggerFactory.getLogger(EnumColumn.class);
    private String clazz;
    private Enum<?>[] items;

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
        try {
            this.items = (Enum<?>[]) Class.forName(clazz).getEnumConstants();
        } catch (ClassNotFoundException e) {
            log.error("枚举获取失败：{}", this.clazz, e);
        }
    }

    @Override
    public Object getValue() {
        String key = this.getString();
        if (key == null) {
            key = "";
        }
        try {
            String val = items[Integer.parseInt(key)].name();
            return val != null ? val : key;
        } catch (Exception e) {
            return key;
        }
    }
}
