package cn.cerc.mis.security;

import cn.cerc.db.core.IHandle;

public class PassportRecord {
    private IHandle handle;
    private String permission;

    public PassportRecord(IHandle handle, String permission) {
        super();
        this.handle = handle;
        this.permission = permission;
    }

    public PassportRecord(IHandle handle, Enum<?> define) {
        super();
        this.handle = handle;
        this.permission = define.name().replaceAll("_", ".");
    }

    public boolean isAdmin() {
        OperatorData data = new OperatorData(Permission.ADMIN);
        return SecurityPolice.check(handle, data.setVersion(handle.getSession().getVersion()));
    }

    public boolean isExecute() {
        OperatorData data = new OperatorData(permission);
        return SecurityPolice.check(handle, data.setVersion(handle.getSession().getVersion()));
    }

    public boolean isPrint() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.REPORT));
    }

    public boolean isOutput() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.EXPORT));
    }

    public boolean isAppend() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.INSERT));
    }

    public boolean isModify() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.UPDATE));
    }

    public boolean isDelete() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.DELETE));
    }

    public boolean isFinish() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.FINISH));
    }

    public boolean isCancel() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.CANCEL));
    }

    public boolean isRecycle() {
        return SecurityPolice.check(handle, this.buildData().add(Operators.NULLIFY));
    }

    private OperatorData buildData() {
        return new OperatorData(permission, true).setVersion(handle.getSession().getVersion());
    }
}
