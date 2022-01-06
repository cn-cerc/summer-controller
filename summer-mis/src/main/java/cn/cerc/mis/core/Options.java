package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.IOption;

public class Options {

    @Deprecated
    public static String getString(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return option.getValue(handle);
    }

    @Deprecated
    public static boolean isOn(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return IOption.ON.equals(option.getValue(handle));
    }

}
