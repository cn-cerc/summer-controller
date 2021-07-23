package cn.cerc.mis.cdn;

import cn.cerc.core.ClassConfig;
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

}
