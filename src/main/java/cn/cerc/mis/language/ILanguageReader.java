package cn.cerc.mis.language;

import java.util.Map;

import cn.cerc.db.core.IHandle;

public interface ILanguageReader {
    /**
     * 取得指定语言的全部对照记录，并存入到items
     * 
     * @param handle IHandle
     * @param items  语言键值对照列表
     * @param langId 语言类型
     * @return 字典对照对照的id
     */
    public int loadDictionary(IHandle handle, Map<String, String> items, String langId);

    /**
     * 取得指定key对应的文字，若字典库不存在则写入
     * 
     * @param handle IHandle
     * @param langId 语言Id
     * @param key    语言查找key
     * @return 字典对照内容
     */
    String getOrSet(IHandle handle, String langId, String key);

}
