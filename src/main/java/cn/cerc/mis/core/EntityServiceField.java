package cn.cerc.mis.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import cn.cerc.db.core.Utils;

public class EntityServiceField {

    private Field field;
    private String alias;

    public EntityServiceField(Field field) {
        this.field = field;
    }

    public EntityServiceField(Field field, String alias) {
        this.field = field;
        this.alias = alias;
    }

    public Field getField() {
        return field;
    }

    public EntityServiceField setField(Field field) {
        this.field = field;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public EntityServiceField setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getName() {
        if (Utils.isEmpty(getAlias()))
            return getField().getName();
        return getAlias();
    }

    public <T extends Annotation> T getAnnotation(Class<T> clazz) {
        return field.getAnnotation(clazz);
    }

    public Class<?> getType() {
        return field.getType();
    }

}
