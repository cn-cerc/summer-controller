package cn.cerc.mis.core;

public interface IWriter {

    IWriter println(Object value);

    IWriter print(Object value);

    IWriter print(String format, Object... args);

    IWriter println(String format, Object... args);

}
