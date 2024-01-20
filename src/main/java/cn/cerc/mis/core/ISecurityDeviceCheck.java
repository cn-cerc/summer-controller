package cn.cerc.mis.core;

import cn.cerc.db.core.IHandle;

public interface ISecurityDeviceCheck extends IHandle {

    SecurityDevice pass(IForm form);
}
