package cn.cerc.mis.ado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;

public class FindOneBatch<T> implements IHandle {
    private Map<String, Optional<T>> buffer = new HashMap<>();
    private FindOneSupplierImpl<Optional<T>> findOne;
    private FindAllSupplierImpl<T> findAll;
    private List<Object[]> keys;
    private int keysSize;
    private ISession session;
    private boolean active;

    public FindOneBatch(IHandle handle, FindOneSupplierImpl<Optional<T>> findOne) {
        this.setSession(handle.getSession());
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

    /**
     * 
     * @return 在满足转化条件的情况下，将List<Object[]>转为List<String>
     */
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

    public void put(Optional<T> value, Object... keys) {
        buffer.put(createId(keys), value);
    }

    public Optional<T> get(Object... keys) {
        init();
        String key = createId(keys);
        Optional<T> result = buffer.get(key);
        if (result == null) {
            result = findOne.get(keys);
            buffer.put(key, result);
        }
        return result;
    }

    private void init() {
        if (active)
            return;
        if (keys != null && keys.size() > 0 && findAll != null)
            findAll.load(this);
        active = true;
    }

    public void onInit(FindAllSupplierImpl<T> findAll) {
        this.findAll = findAll;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    public String createId(Object... values) {
        StringBuffer sb = new StringBuffer();
        for (Object value : values)
            sb.append(value);
        return sb.toString();
    }
}