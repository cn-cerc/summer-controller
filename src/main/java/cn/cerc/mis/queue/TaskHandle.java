package cn.cerc.mis.queue;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;
import cn.cerc.mis.security.CustomSession;

public class TaskHandle implements IHandle, AutoCloseable {
    private ISession session;

    public TaskHandle() {
        super();
        session = new CustomSession();
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /**
     * 从指定帐套生成token，如果指定帐套存在指定的用户，则使用指定用户生成token，否则使用这个帐套有效用户中的第一个帐号生成token
     * 
     * @param corpNo   指定帐套的代码
     * @param userCode 指定用户的代码
     */
    public void initToken(String corpNo, String userCode) {
        this.getSession().setProperty(ISession.CORP_NO, corpNo);
        this.getSession().setProperty(ISession.USER_CODE, userCode);
    }

}
