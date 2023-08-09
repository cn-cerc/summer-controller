package cn.cerc.mis.core;

import java.util.List;

import javax.persistence.Column;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.Describe;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.Utils;
import cn.cerc.mis.ado.CustomEntity;
import cn.cerc.mis.ado.EmptyEntity;
import cn.cerc.mis.core.StubEntityService.BodyInEntity;
import cn.cerc.mis.core.StubEntityService.HeadInEntity;

public class StubEntityService extends CustomEntityService<HeadInEntity, BodyInEntity, EmptyEntity, EmptyEntity> {
    public class BodyInEntity extends CustomEntity {
        @Column
        @Describe(name = "单号", width = 20)
        private String tbNo_;
        @Column
        @Describe(name = "单序", width = 4)
        private Integer it_;

        public String getTbNo_() {
            return tbNo_;
        }

        public void setTbNo_(String tbNo_) {
            this.tbNo_ = tbNo_;
        }

        public Integer getIt_() {
            return it_;
        }

        public void setIt_(Integer it_) {
            this.it_ = it_;
        }

    }

    public class HeadInEntity extends CustomEntity {
        @Column
        @Describe(name = "单号", width = 20)
        private String tbNo_;

        public String getTbNo_() {
            return tbNo_;
        }

        public void setTbNo_(String tbNo_) {
            this.tbNo_ = tbNo_;
        }
    }

    @Override
    public DataSet process(IHandle handle, HeadInEntity headIn, List<BodyInEntity> bodyIn) {
//        headIn.getTbNo_();
//        for(var body: bodyIn) {
//            body.getTbNo_();
//            body.getIt_();
//        }
        return new DataSet().setOk();
    }

    @Override
    protected void validateHeadIn(HeadInEntity head) throws DataValidateException {
        DataValidateException.stopRun("单头单号不允许为空", Utils.isEmpty(head.getTbNo_()));
    }

    @Override
    protected void validateBodyIn(BodyInEntity body) throws DataValidateException {
        DataValidateException.stopRun("单身单号不允许为空", Utils.isEmpty(body.getTbNo_()));
        DataValidateException.stopRun("单身单序不允许为零", body.getIt_() == 0);
    }

    @Override
    protected Class<HeadInEntity> getHeadInClass() {
        return HeadInEntity.class;
    }

    @Override
    protected Class<BodyInEntity> getBodyInClass() {
        return BodyInEntity.class;
    }

    @Override
    protected Class<EmptyEntity> getHeadOutClass() {
        return null;
    }

    @Override
    protected Class<EmptyEntity> getBodyOutClass() {
        return null;
    }

}
