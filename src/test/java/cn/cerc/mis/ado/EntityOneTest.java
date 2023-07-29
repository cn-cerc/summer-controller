package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.junit.Test;

import com.google.gson.Gson;

public class EntityOneTest {
    private Consumer<StubEntity> doInsert = (item) -> {
        item.setUID_(1l);
        item.setCode_("a02");
    };

    @Test
    public void test_findRecNo() {
        StubEntity entity = new StubEntity();
        assertEquals(entity.findRecNo(), -1);
        var query = new EntityOne<StubEntity>(StubEntity.class);
        entity = query.insert(doInsert);
        assertEquals(entity.findRecNo(), 1);
        query.delete();
        assertEquals(entity.findRecNo(), 0);
    }

    @Test
    public void test_QueryOne() {
        var query = new EntityOne<StubEntity>(StubEntity.class);
        assertTrue(query.isEmpty());
        StubEntity entity = query.orElseInsert(doInsert);
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
