package cn.cerc.mis.core;

import java.util.List;

public interface IEntityServiceFields {

    public List<EntityServiceField> getHeadInFields();

    public List<EntityServiceField> getBodyInFields();

    public List<EntityServiceField> getHeadOutFields();

    public List<EntityServiceField> getBodyOutFields();

}
