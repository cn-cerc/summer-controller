package cn.cerc.mis.core;

@Deprecated
public interface IOptionReader {

    String getUserValue(String userCode, String optionKey, String defaultValue);

}
