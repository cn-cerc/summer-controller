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

    public void setCorpNo(String corpNo) {
        session.setProperty(ISession.CORP_NO, corpNo);
    }

}
