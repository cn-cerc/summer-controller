package cn.cerc.mis.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.mis.client.ServiceExecuteException;

public class FormQuery {
    private static final Logger log = LoggerFactory.getLogger(FormQuery.class);

    public static String call(AbstractForm owner, String id, String... pathVariables)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, ServletException, IOException, ServiceExecuteException {
        FormSign sv = new FormSign(id);
        String formId = sv.getId();
        String funcCode = sv.getValue();
        if (!formId.substring(0, 2).toUpperCase().equals(formId.substring(0, 2)))
            formId = formId.substring(0, 1).toLowerCase() + formId.substring(1);
        AbstractForm bean = Application.getContext().getBean(formId, AbstractForm.class);
        bean.setSession(owner.getSession());
        bean.setId(formId);
        bean.setPathVariables(pathVariables);
        try {
            return bean._call(funcCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
