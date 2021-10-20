package cn.cerc.mis.custom;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cn.cerc.core.DataRow;
import cn.cerc.core.ISession;
import cn.cerc.core.LanguageResource;
import cn.cerc.db.core.Handle;
import cn.cerc.db.redis.JedisFactory;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.CenterService;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
import cn.cerc.mis.security.CustomSession;
import redis.clients.jedis.Jedis;

@Component
//@Scope(WebApplicationContext.SCOPE_REQUEST)
//@Scope(WebApplicationContext.SCOPE_SESSION)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SessionDefault extends CustomSession {
    private static final Logger log = LoggerFactory.getLogger(SessionDefault.class);

    public SessionDefault() {
        super();
        params.put(Application.SessionId, "");
        params.put(Application.ProxyUsers, "");
        params.put(ISession.CORP_NO, "");
        params.put(Application.ClientIP, "0.0.0.0");
        params.put(ISession.LANGUAGE_ID, LanguageResource.appLanguage);
    }

    @Override
    public boolean logon() {
        if (this.getProperty(ISession.TOKEN) == null)
            return false;
        String corpNo = this.getCorpNo();
        return corpNo != null && !"".equals(corpNo);
    }

    @Override
    public final String getCorpNo() {
        return (String) this.getProperty(ISession.CORP_NO);
    }

    @Deprecated
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public void loadToken(String token) {
        params.put(TOKEN, token);
        if (token == null)
            return;
        if (token.length() < 10)
            throw new RuntimeException("token value error: length < 10");

        try (MemoryBuffer buff = new MemoryBuffer(SystemBuffer.Token.SessionBase, token);
                Jedis redis = JedisFactory.getJedis()) {
            if (buff.isNull() || !buff.getBoolean("exists")) {
                CenterService svr = new CenterService(new Handle(this));
                svr.setService("SvrSession.byToken");
                if (!svr.exec("token", token)) {
                    log.debug("token restore error：{}", svr.getMessage());
                    params.put(ISession.TOKEN, null);
                    return;
                }
                DataRow record = svr.getDataOut().getHead();
                buff.setValue("LoginTime_", record.getDatetime("LoginTime_"));
                buff.setValue("UserID_", record.getString("UserID_"));
                buff.setValue("UserCode_", record.getString("UserCode_"));
                buff.setValue("CorpNo_", record.getString("CorpNo_"));
                buff.setValue("UserName_", record.getString("UserName_"));
                buff.setValue("RoleCode_", record.getString("RoleCode_"));
                buff.setValue("ProxyUsers_", record.getString("ProxyUsers_"));
                buff.setValue("Language_", record.getString("Language_"));
                buff.setValue("exists", true);
            }

            if (buff.getBoolean("exists")) {
                // 将用户信息赋值到句柄
                params.put(Application.LoginTime, buff.getDatetime("LoginTime_"));
                params.put(ISession.CORP_NO, buff.getString("CorpNo_"));
                params.put(Application.UserId, buff.getString("UserID_"));
                params.put(ISession.USER_CODE, buff.getString("UserCode_"));
                params.put(ISession.USER_NAME, buff.getString("UserName_"));
                params.put(Application.ProxyUsers, buff.getString("ProxyUsers_"));
                params.put(ISession.LANGUAGE_ID, buff.getString("Language_"));

                // 刷新缓存生命值
                if (redis != null)
                    redis.expire(buff.getKey(), buff.getExpires());
            }
        }
    }

}
