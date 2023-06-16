package cn.cerc.mis.ado;

import java.util.Objects;

import cn.cerc.db.core.EntityHomeImpl;
import cn.cerc.db.core.EntityImpl;

public class CustomEntity implements EntityImpl {
    private transient EntityHomeImpl entityHome;
    private boolean locked = true;

    /**
     * 设置EntityQuery
     * 
     * @param entityHome EntityQuery
     */
    @Override
    public void setEntityHome(EntityHomeImpl entityHome) {
        this.entityHome = entityHome;
    }

    @Override
    public EntityHomeImpl getEntityHome() {
        return entityHome;
    }

    /**
     * 注意：若EntityQuery不存在，则返回-1
     * 
     * @return 返回自身在 EntityQuery 中的序号，从1开始，若没有找到，则返回0
     */
    @Override
    public int findRecNo() {
        if (entityHome != null)
            return entityHome.findRecNo(this);
        else
            return -1;
    }

    @Override
    public void refresh() {
        Objects.requireNonNull(entityHome, "entityHome is null");
        entityHome.refresh(this);
    }

    /**
     * 提交到 EntityQuery
     */
    @Override
    public void post() {
        Objects.requireNonNull(entityHome, "entityHome is null");
        entityHome.post(this);
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }
}
