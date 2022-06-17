package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.FastDate;

public class EntityCacheTest {

    @Test
    public void test_buildKey() {
        String[] keys = new String[2];
        keys[0] = "a";
        keys[1] = "2";
        assertEquals("70.a.2", EntityCache.buildKey(keys));
    }

    @Test
    public void test_joinString() {
        assertEquals("a.2", String.join(".", "a", "2"));
        assertEquals("a.1", String.join(".", "a", "1"));
        assertEquals("a.2.0", String.join(".", "a", "2", "0"));
        assertEquals("a.2.0.null", String.join(".", "a", "2", "0", null));
        assertEquals("a.2.0..2021-01-01", String.join(".", "a", "2", "0", "", new FastDate("2021-01-01").toString()));
    }

    @Test
    public void test_buildKeys() {
        EntityCache<SampleEntity> ec = new EntityCache<SampleEntity>(null, SampleEntity.class);
        String clazzName = SampleEntity.class.getSimpleName();
        assertEquals(clazzName + ".a.0", String.join(".", ec.buildKeys("a", "0")));
        assertEquals(clazzName + ".a.null", String.join(".", ec.buildKeys("a", null)));
        DataRow row = new DataRow();
        row.setValue("corpNo_", "a");
        row.setValue("enanble_", true);
        assertEquals(clazzName + ".a.true", String.join(".", ec.buildKeys(row)));
    }

    @Test
    public void test_getVirtualEntity() {
        EntityCache<SampleEntity> ec = new EntityCache<SampleEntity>(null, SampleEntity.class);
        SampleEntity entity = ec.getVirtualEntity("a", "1");
        assertTrue(entity == null);
    }

}
