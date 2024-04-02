package cn.cerc.mis.cdn;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.SummerMIS;

public class CDN {
    private static final ClassConfig config = new ClassConfig(CDN.class, SummerMIS.ID);
    // 启用内容网络分发
    @Deprecated
    public static final String OSS_CDN_ENABLE = "oss.cdn.enable";
    // 浏览器缓存版本号
    public static final String BROWSER_CACHE_VERSION = "browser.cache.version";

    public static String get(String file) {
        return file + "?v=" + config.getString(BROWSER_CACHE_VERSION, "1.0.0.0");
    }

    /**
     * TODO 改为从zookeeper读取配置
     */
    @Deprecated
    public static String getSite() {
        String site = config.getProperty("cdn.site", "");
        if (Utils.isEmpty(site))
            throw new RuntimeException("CDN 地址没有配置");
        return site;
    }

}
