package cn.cerc.mis.ado;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.EntityHelper;
import cn.cerc.db.core.EntityHomeImpl;
import cn.cerc.db.core.EntityImpl;

public class CustomEntity implements EntityImpl {
    private static final Logger log = LoggerFactory.getLogger(CustomEntity.class);
    private transient EntityHomeImpl entityHome;

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
    public boolean isLocked() {
        boolean result = EntityImpl.super.isLocked();
        EntityHelper<? extends CustomEntity> helper = EntityHelper.create(this.getClass());
        var field = helper.lockedField();
        if (field.isPresent()) {
            try {
                Object data = field.get().get(this);
                if (data instanceof Boolean value)
                    result = value;
                else
                    log.error("locked field type is not Boolean");
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("isLocked error: {}", e.getMessage());
            }
        }
        return result;
    }
}
