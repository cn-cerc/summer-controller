package cn.cerc.mis.core;

public interface ISupplierForm {

    boolean findForm(String formId, String funcCode);

    IForm getForm();

}
