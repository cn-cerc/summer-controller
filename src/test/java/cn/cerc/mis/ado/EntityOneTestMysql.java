package cn.cerc.mis.ado;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.cerc.db.core.Handle;
import cn.cerc.db.testsql.TestsqlServer;

public class EntityOneTestMysql {

    @Test
    public void test_testsql() {
        var jsonText = """
                {"body":[["UID_","corpNo_","Code_","enanble_","amount_"],[1,"000000","001",true,0]]}""";
        var db = TestsqlServer.build();
        db.onSelect(UserTest.Table, (query, sql) -> {
            query.setJson(jsonText);
        });
        var query = EntityOne.open(Handle.getStub(), UserTest.class);
        assertEquals(1, db.tables().size());
        assertEquals(jsonText, db.tables().get(UserTest.Table).toString());
        assertEquals(jsonText, query.dataSet().toString());
    }

}
