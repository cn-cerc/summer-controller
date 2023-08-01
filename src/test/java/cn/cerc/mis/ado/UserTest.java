package cn.cerc.mis.ado;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import cn.cerc.db.core.EntityHomeImpl;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;

@Entity
@javax.persistence.Table(name = UserTest.Table)
@EntityKey(fields = { "Code_" }, corpNo = false)
public class UserTest implements EntityImpl {
    public static final String Table = "s_user";

    @Id
    public String id_;
    @Column(name = "Code_")
    public String code;
    @Column(name = "Name_")
    public String name;
    @Column(name = "Mobile_")
    public String mobile;
    @Version
    public Integer version_;

    @Override
    public EntityHomeImpl getEntityHome() {
        return null;
    }

    @Override
    public void setEntityHome(EntityHomeImpl entityHome) {

    }

}