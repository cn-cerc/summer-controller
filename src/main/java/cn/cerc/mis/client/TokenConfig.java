package cn.cerc.mis.client;

import java.util.Optional;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;

public class TokenConfig implements TokenConfigImpl {
    private String token;
//    private String original;
    private ISession session;

    public TokenConfig(IHandle handle, String token) {
        this.session = handle.getSession();
        this.token = token;
    }
//
//    @Override
//    public Optional<String> getToken() {
//        return Optional.ofNullable(token);
//    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }
//
//    @Override
//    public Optional<String> getOriginal() {
//        return Optional.ofNullable(original);
//    }
//
//    public void setOriginal(String original) {
//        this.original = original;
//    }

    @Override
    public Optional<String> getBookNo() {
        return Optional.empty();
    }
}
