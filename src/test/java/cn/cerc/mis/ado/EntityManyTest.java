package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import com.google.gson.Gson;

import cn.cerc.db.core.Handle;
import cn.cerc.db.core.StubSession;

public class EntityManyTest {
    private Consumer<StubEntity> doInsert = (item) -> {
        item.setUID_(1l);
        item.setCode_("a02");
    };

    @Test
    public void test_findRecNo() {
        StubEntity entity = new StubEntity();
        assertEquals(entity.findRecNo(), -1);
        var query = new EntityMany<StubEntity>(new Handle(new StubSession()), StubEntity.class);
        entity = query.insert(doInsert);
        assertEquals(entity.findRecNo(), 1);
        query.deleteAll();
        assertEquals(entity.findRecNo(), 0);
    }

    @Test
    public void test_QueryAll() {
        var query = new EntityMany<StubEntity>(new Handle(new StubSession()), StubEntity.class);
        assertTrue(query.isEmpty());
        StubEntity entity = query.insert(doInsert);
        assertEquals(entity.findRecNo(), 1);
        assertEquals("000000", entity.getCorpNo_());
        assertTrue(query.isPresent());
        String json = new Gson().toJson(entity);
        assertEquals("{\"UID_\":1,\"corpNo_\":\"000000\",\"Code_\":\"a02\",\"enanble_\":true,\"amount_\":0.0}", json);
        query.update(item -> item.setCode_("a02"));
        query.deleteIf(item -> item.getEnanble_());
        assertEquals("a02", entity.getCode_());
        assertEquals(entity.findRecNo(), 0);
        query.insert(List.of(entity));
        query.deleteIf(item -> item.getEnanble_());
        assertEquals(entity.findRecNo(), 0);
    }

    @Test
    public void test_insert() {
        var query = new EntityMany<StubEntity>(new Handle(new StubSession()), StubEntity.class);
        List<StubEntity> list = new ArrayList<>();
        StubEntity entity1 = new StubEntity();
        entity1.setUID_(1l);
        entity1.setCode_("a01");
        list.add(entity1);
        StubEntity entity2 = new StubEntity();
        entity2.setUID_(2l);
        entity2.setCode_("a02");
        list.add(entity2);
        StubEntity entity3 = new StubEntity();
        entity3.setUID_(3l);
        entity3.setCode_("a03");
        list.add(entity3);
        query.insert(list);
        assertEquals(query.size(), 3);
        assertEquals(entity1.findRecNo(), 1);
        assertEquals(entity2.findRecNo(), 2);
        assertEquals(entity3.findRecNo(), 3);
        query.deleteAll(List.of(entity2));
        System.out.println(query.dataSet());
        assertEquals(entity1.findRecNo(), 1);
        assertEquals(entity2.findRecNo(), 0);
        entity3.setAmount_(1d);
        entity3.post();
        System.out.println(query.dataSet());
        assertEquals(entity3.findRecNo(), 2);
    }
}
