package cn.cerc.mis.ado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import cn.cerc.db.core.Utils;

public class BatchCache<T> {
    private Map<String, Optional<T>> buffer = new HashMap<>();
    private ISupplierFindOne<Optional<T>> findOne;
    private ISupplierFindBatch<T> findAll;
    private List<Object[]> keys;
    private int keysSize;
    private boolean active;

    public BatchCache(ISupplierFindOne<Optional<T>> findOne) {
        this.findOne = findOne;
    }

    public void prepare(Object... keys) {
        if (keys.length == 0)
            throw new IllegalArgumentException("keys is null");
        if (keysSize == 0)
            keysSize = keys.length;
        else if (keysSize != keys.length)
            throw new IllegalArgumentException("keys size not change");
        this.keys().add(keys);
        active = false;
    }

    public List<Object[]> keys() {
        if (this.keys == null)
            this.keys = new ArrayList<>();
        return keys;
    }

    // 在满足转化条件的情况下，将List<Object[]>转为List<String>
    public List<String> keyList() {
        List<String> items = new ArrayList<>();
        if (keys != null && keys.size() > 0) {
            if (keysSize != 1)
                throw new IllegalArgumentException("keysLength != 1");
            if (!(keys.get(0)[0] instanceof String))
                throw new IllegalArgumentException("key not is String");
            this.keys().forEach(item -> items.add((String) item[0]));
        }
        return items;
    }

    public void put(Optional<T> value, String... keys) {
        buffer.put(String.join(".", keys), value);
    }

    public Optional<T> get(String... keys) {
        init();
        String key = String.join(".", keys);
        Optional<T> result = buffer.get(key);
        if (result == null) {
            result = findOne.get(keys);
            buffer.put(key, result);
        }
        return result;
    }

    public String getOrDefault(Function<? super T, String> mapper, String key) {
        if (Utils.isEmpty(key))
            return "";
        Optional<T> entity = get(key);
        return entity.isEmpty() ? key : mapper.apply(entity.get());
    }

    private void init() {
        if (active)
            return;
        if (keys != null && keys.size() > 0 && findAll != null)
            findAll.load(this);
        active = true;
    }

    public void onInit(ISupplierFindBatch<T> findAll) {
        this.findAll = findAll;
    }

}
