package cn.cerc.mis.core;

public interface ISupplierForm {

    boolean findForm(String beanId, String funcCode);

    IForm getForm();

}
