package cn.cerc.mis.ado;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.SqlServerType;
import cn.cerc.db.core.SqlText;
import cn.cerc.mis.ado.EntityCache.VirtualEntityImpl;

@Entity
@EntityKey(fields = { "corpNo_", "enanble_" })
public class SampleEntity extends CustomEntity implements VirtualEntityImpl {
    private static final Logger log = LoggerFactory.getLogger(SampleEntity.class);
    @Id
    private Long UID_;
    @Column(nullable = false)
    @Describe(name = "公司别", def = "000000")
    private String corpNo_;
    @Column
    private String Code_;
    @Column
    @Describe(name = "状态", def = "1")
    private Boolean enanble_;
    @Column(nullable = false)
    private Double amount_;

    public SampleEntity() {
        super();
    }

    public SampleEntity(String code) {
        super();
        this.Code_ = code;
    }

    public Long getUID_() {
        return UID_;
    }

    public void setUID_(Long uID_) {
        UID_ = uID_;
    }

    public String getCorpNo_() {
        return corpNo_;
    }

    public void setCorpNo_(String corpNo_) {
        this.corpNo_ = corpNo_;
    }

    public String getCode_() {
        return Code_;
    }

    public void setCode_(String code_) {
        Code_ = code_;
    }

    public Boolean getEnanble_() {
        return enanble_;
    }

    public void setEnanble_(Boolean enanble_) {
        this.enanble_ = enanble_;
    }

    public Double getAmount_() {
        return amount_;
    }

    public void setAmount_(Double amount_) {
        this.amount_ = amount_;
    }

    @Override
    public boolean fillItem(IHandle handle, Object entity, DataRow headIn) {
        return false;
    }

    @Override
    public DataSet loadItems(IHandle handle, DataRow headIn) {
        return null;
    }

    public static void main(String[] args) {
        EntityQueryOne<SampleEntity> loadOne = EntityFactory.loadOne(null, SampleEntity.class,
                new SqlText(SqlServerType.Mysql));
        // 找不到就抛错，否则就执行更新
        loadOne.isEmptyThrow(() -> new RuntimeException("找不到相应的记录"));
        // 找到就抛错，否则就执行插入
        loadOne.isPresentThrow(() -> new RuntimeException("记录已经存在，不允许再增加")).orElseInsert(item -> {
            item.setCode_("a01");
        });
        // 找到就更新，否则就执行插入
        loadOne.update(item -> item.setAmount_(item.getAmount_() + 1)).orElseInsert(item -> {
            item.setCode_("a01");
        });
        // 找不到就抛错，否则予以删除
        loadOne.isEmptyThrow(() -> new RuntimeException("找不到相应的记录")).delete();
        // 能删除就删除，删除后打印日志
        SampleEntity entity = loadOne.delete();
        if (entity != null)
            log.info("删除了记录：" + entity.getCode_());
        // 取出entity并修改，找不到就抛错
        entity = loadOne.getElseThrow(() -> new RuntimeException("找不到相应的记录"));
        entity.setAmount_(entity.getAmount_() + 1);
        entity.post();

        // 对于多条记录的处理
        EntityQueryAll<SampleEntity> loadAll = EntityFactory.loadAll(null, SampleEntity.class,
                new SqlText(SqlServerType.Mysql));
        // 插入一条件
        loadAll.insert(item -> {
            item.setCode_("a01");
        });
        // 如果为空就插入1条记录
        if (loadAll.isEmpty()) {
            loadAll.insert(item -> {
                item.setCode_("a001");
            });
        }
        // 一次性插入3条记录
        List<SampleEntity> list = new ArrayList<>();
        list.add(new SampleEntity("a01"));
        list.add(new SampleEntity("a02"));
        list.add(new SampleEntity("a03"));
        loadAll.insert(list);
        // 如果返回值为真则更新记录：
        for (SampleEntity item : loadAll) {
            if (item.getAmount_() < 100) {
                item.setAmount_(item.getAmount_() + 1);
                item.post();
            }
        }
        // 如果返回值为真则更新记录，传统写法:
        for (int i = 0; i < loadAll.size(); i++) {
            SampleEntity item = loadAll.get(i);
            if (item.getAmount_() < 100) {
                item.setAmount_(item.getAmount_() + 1);
                item.post();
            }
        }
        // 更新所有的记录
        loadAll.updateAll(item -> item.setAmount_(item.getAmount_() + 1));

        // 删除所有金额小于100的记录
        loadAll.deleteIf(item -> item.getAmount_() < 100);
        // 删除所有记录
        loadAll.deleteAll();

        // 修改一部分，删除一部分
        List<SampleEntity> delList = new ArrayList<>();
        for (SampleEntity item : loadAll) {
            if (item.getAmount_() < 100) {
                item.setAmount_(item.getAmount_() + 1);
                item.post();
            } else {
                delList.add(item);
            }
        }
        loadAll.deleteAll(delList);
    }

}
