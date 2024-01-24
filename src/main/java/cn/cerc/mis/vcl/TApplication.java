package cn.cerc.mis.vcl;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于建立客户端窗口
 *
 * @author 张弓
 */
public class TApplication {
    private static final Logger log = LoggerFactory.getLogger(TApplication.class);

    private TCustomForm mainForm;

    public TCustomForm createForm(Class<?> clazz) {
        try {
            mainForm = (TCustomForm) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            log.error(e.getMessage(), e);
        }
        return mainForm;
    }

    public void run() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainForm.setVisible(true);
            }
        });
    }

    public TCustomForm getMainForm() {
        return mainForm;
    }

    public void setMainForm(TCustomForm mainForm) {
        this.mainForm = mainForm;
    }

    public static void main(String[] args) {
        TApplication app = new TApplication();
        app.createForm(TCustomForm.class);
        app.run();
    }

}
