package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.junit.Test;

import com.google.gson.Gson;

import cn.cerc.db.core.SqlText;

public class EntityQueryOneTest {
    private Consumer<SampleEntity> doInsert = (item) -> {
        item.setUID_(1l);
        item.setCode_("a02");
    };

    private EntityQueryOne<SampleEntity> findOne() {
        SqlText sql = null;
        return new EntityQueryOne<SampleEntity>(null, SampleEntity.class, sql, false, false);
    }

    @Test
    public void test_findRecNo() {
        SampleEntity entity = new SampleEntity();
        assertEquals(entity.findRecNo(), -1);
        EntityQueryOne<SampleEntity> query = findOne();
        entity = query.insert(doInsert);
        assertEquals(entity.findRecNo(), 1);
        query.delete();
        assertEquals(entity.findRecNo(), 0);
    }

    @Test
    public void test_QueryOne() {
        EntityQueryOne<SampleEntity> query = findOne();
        assertTrue(query.isEmpty());
        SampleEntity entity = query.orElseInsert(doInsert);
        assertEquals(entity.findRecNo(), 1);
        assertEquals("000000", entity.getCorpNo_());
        assertTrue(query.isPresent());
        String json = new Gson().toJson(entity);
        assertEquals("{\"UID_\":1,\"corpNo_\":\"000000\",\"Code_\":\"a02\",\"enanble_\":true,\"amount_\":0.0}", json);
        query.update(item -> item.setCode_("a02"));

        entity = query.delete();
        assertEquals("a02", entity.getCode_());
        assertEquals(entity.findRecNo(), -1);
        assertEquals(query.delete(), null);
    }

}
