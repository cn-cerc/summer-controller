package cn.cerc.mis.custom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import cn.cerc.db.core.Handle;
import cn.cerc.db.core.ISession;
import cn.cerc.db.core.Utils;
import cn.cerc.db.mysql.MysqlQuery;
import cn.cerc.mis.cache.ISessionCache;
import cn.cerc.mis.core.IAppLanguage;
import cn.cerc.mis.core.ISystemTable;

@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppLanguageDefault implements IAppLanguage, ISessionCache {
    private static final Logger log = LoggerFactory.getLogger(AppLanguageDefault.class);
    @Autowired
    private ISystemTable systemTable;
    // 存储每个用户的设置值
    private Map<String, String> items = new ConcurrentHashMap<>();

    @Override
    public String getLanguageId(ISession session, String defaultValue) {
        String result = defaultValue;
        String userCode = session.getUserCode();
        if (Utils.isEmpty(userCode))
            return result;

        if (items.containsKey(userCode))
            return items.get(userCode);

        synchronized (this) {
            try {
                MysqlQuery ds = new MysqlQuery(new Handle(session));
                ds.add("select Value_ from %s", systemTable.getUserOptions());
                ds.add("where Code_='%s' and UserCode_='%s'", "Lang_", userCode);
                ds.open();
                if (!ds.eof()) {
                    result = ds.getString("Value_");
                }
                items.put(userCode, result);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void clearCache() {
        this.items.clear();
    }

}