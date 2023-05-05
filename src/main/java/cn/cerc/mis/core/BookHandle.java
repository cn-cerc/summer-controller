package cn.cerc.mis.core;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;

@Deprecated
public class BookHandle implements IHandle {
    private ISession session;
    private Map<String, Object> params = new HashMap<>();
    private String originCorpNo;
    private String originUserCode;

    public BookHandle(IHandle handle, String corpNo) {
        init(handle.getSession(), corpNo);
    }

    public BookHandle(ISession owner, String corpNo) {
        init(owner, corpNo);
    }

    private void init(ISession owner, String corpNo) {
        this.originCorpNo = owner.getCorpNo();
        this.originUserCode = owner.getUserCode();
        this.session = new ISession() {
            @Override
            public Object getProperty(String key) {
                if (params.containsKey(key))
                    return params.get(key);
                else
                    return owner.getProperty(key);
            }

            @Override
            public void setProperty(String key, Object value) {
                params.put(key, value);
            }

            @Override
            public boolean logon() {
                return owner.logon();
            }

            @Override
            public void close() {
                owner.close();
            }

            @Override
            public void loadToken(String token) {
                throw new RuntimeException("not support loadToken");
            }

            @Override
            public HttpServletRequest getRequest() {
                return owner.getRequest();
            }

            @Override
            public void setRequest(HttpServletRequest request) {

            }

            @Override
            public HttpServletResponse getResponse() {
                return owner.getResponse();
            }

            @Override
            public void setResponse(HttpServletResponse response) {

            }

            @Override
            public String getPermissions() {
                return owner.getPermissions();
            }
        };

        this.session.setProperty(ISession.CORP_NO, corpNo);
    }

    public BookHandle setUserCode(String userCode) {
        session.setProperty(ISession.USER_CODE, userCode);
        return this;
    }

    public BookHandle setUserName(String userName) {
        session.setProperty(ISession.USER_NAME, userName);
        return this;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        throw new RuntimeException("BookHandle not support setSession.");
    }

    public String getOriginCorpNo() {
        return originCorpNo;
    }

    public String getOriginUserCode() {
        return originUserCode;
    }

}
