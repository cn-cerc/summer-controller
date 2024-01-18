package cn.cerc.mis.core;

public interface IUserMenuCheck {

    MenuCheckRecord permit(IForm form);

    record MenuCheckRecord(boolean result, String message) {
    }

}
