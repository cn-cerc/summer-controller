package cn.cerc.mis.ado;

import javax.persistence.Entity;

import cn.cerc.db.core.CacheLevelEnum;
import cn.cerc.db.core.EntityKey;

@Entity
@EntityKey(fields = { "field1", "field2" }, cache = CacheLevelEnum.Redis)
public class MockFamilyEntity extends CustomEntity {

    @EntityKey(fields = { "field1", "field2" }, cache = CacheLevelEnum.Redis)
    public static class IX_1 extends MockFamilyEntity {

    }

    public static class IX_2 extends MockFamilyEntity {

    }

}
