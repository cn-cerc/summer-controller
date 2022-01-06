package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.IOption;

public class Options {

    public static final String ON = "on";
    public static final String OFF = "off";

    public static String getString(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return option.getValue(handle);
    }

    public static boolean isOn(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return Options.ON.equals(option.getValue(handle));
    }

}
