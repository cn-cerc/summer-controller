package cn.cerc.mis.core;

import java.util.ArrayList;
import java.util.List;

import cn.cerc.mis.exception.IKnowall;

/**
 * 记录 BookHandle 的调用者异常
 */
public class BookHandleException extends RuntimeException implements IKnowall {

    private static final long serialVersionUID = 5157802210363657558L;

    private final String corpNo;
    private final String userCode;

    public BookHandleException(String corpNo, String userCode) {
        this.corpNo = corpNo;
        this.userCode = userCode;
    }

    @Override
    public String[] getData() {
        List<String> list = new ArrayList<String>();
        list.add("corpNo: " + corpNo);
        list.add("userCode: " + userCode);
        return list.toArray(new String[0]);
    }

}
