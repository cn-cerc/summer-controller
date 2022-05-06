package cn.cerc.mis.core;

import org.springframework.beans.factory.BeanNameAware;

public interface SupportBeanName extends BeanNameAware {

    String getBeanName();

}
