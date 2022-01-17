package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.FastDate;

public class EntityCacheTest {

    @Test
    public void test_buildKey() {
        Object[] keys = new Object[2];
        keys[0] = "a";
        keys[1] = 2;
        assertEquals("70.a.2", EntityCache.buildKey(keys));
    }

    @Test
    public void test_joinString() {
        assertEquals("a.2", EntityCache.joinToKey("a", 2));
        assertEquals("a.1", EntityCache.joinToKey("a", true));
        assertEquals("a.2.0", EntityCache.joinToKey("a", 2, false));
        assertEquals("a.2.0.", EntityCache.joinToKey("a", 2, false, null));
        assertEquals("a.2.0..2021-01-01", EntityCache.joinToKey("a", 2, false, null, new FastDate("2021-01-01")));
    }

    @Test
    public void test_buildKeys() {
        EntityCache<SampleEntity> ec = new EntityCache<SampleEntity>(null, SampleEntity.class);
        String clazzName = SampleEntity.class.getSimpleName();
        assertEquals(clazzName + ".a.0", EntityCache.joinToKey(ec.buildKeys("a", false)));
        assertEquals(clazzName + ".a.", EntityCache.joinToKey(ec.buildKeys("a", null)));
        DataRow row = new DataRow();
        row.setValue("corpNo_", "a");
        row.setValue("enanble_", true);
        assertEquals(clazzName + ".a.1", EntityCache.joinToKey(ec.buildKeys(row)));
    }

    @Test
    public void test_getVirtualEntity() {
        EntityCache<SampleEntity> ec = new EntityCache<SampleEntity>(null, SampleEntity.class);
        SampleEntity entity = ec.getVirtualEntity("a", true);
        assertTrue(entity == null);
    }

}
