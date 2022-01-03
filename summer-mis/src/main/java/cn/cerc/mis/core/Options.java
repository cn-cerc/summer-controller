package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.IOption;
import cn.cerc.db.core.KeyValue;

public class Options {

    public static final String ON = "on";
    public static final String OFF = "off";

    public static KeyValue get(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return new KeyValue(option.getValue(handle));
    }

    public static KeyValue get(IHandle handle, Class<? extends IOption> clazz, String def) {
        IOption option = Application.getContext().getBean(clazz);
        return new KeyValue(option.getValue(handle, def));
    }

    public static boolean isOn(IHandle handle, Class<? extends IOption> clazz) {
        IOption option = Application.getContext().getBean(clazz);
        return cn.cerc.mis.core.Options.ON.equals(option.getValue(handle, ""));
    }

}
