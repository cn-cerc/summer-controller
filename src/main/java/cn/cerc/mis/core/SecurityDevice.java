package cn.cerc.mis.core;

public enum SecurityDevice {
    forbid, // 禁止使用
    permit, // 验证通过
    login, // 重新登录
    check; // 设备检查
}
