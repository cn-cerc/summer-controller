package cn.cerc.mis.core;

import java.lang.reflect.Field;
import java.util.Map;

import javax.persistence.Column;

public interface IEntityServiceFields {

    public Map<Field, Column> getMetaHeadIn();

    public Map<Field, Column> getMetaBodyIn();

    public Map<Field, Column> getMetaHeadOut();

    public Map<Field, Column> getMetaBodyOut();

}
