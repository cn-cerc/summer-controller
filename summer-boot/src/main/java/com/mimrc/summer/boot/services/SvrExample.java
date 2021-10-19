package com.mimrc.summer.boot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.mimrc.summer.boot.core.AppDB;

import cn.cerc.core.DataRow;
import cn.cerc.core.FieldDefs;
import cn.cerc.db.mysql.MysqlQuery;
import cn.cerc.mis.core.CustomService;
import cn.cerc.mis.core.Permission;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Permission(Permission.GUEST)
public class SvrExample extends CustomService {
    private static final Logger log = LoggerFactory.getLogger(SvrExample.class);

    public boolean search() {
        // 获取外部传进来的数据
        DataRow headIn = getDataIn().getHead();
        log.debug("headIn {}", headIn);

        // MysqlQuery 用于操作数据库，可对数据进行增删改查，在使用增删改功能前，必须查询表。
        MysqlQuery query = new MysqlQuery(this);
        // add方法追加sql语句
        query.add("select * from %s", AppDB.TABLE_EXAMPLE);
        query.add("where 1=1 ");
        // 判断传进来的值，存在code_并且不为空
        if (headIn.hasValue("code_")) {
            query.add("and code_='%s'", headIn.getString("code_"));
        }

        if (headIn.hasValue("searchText_")) {
            String searchText = headIn.getString("searchText_");
            // 此处使用占位符进行%占位
            query.add("and (name_ like '%%%s%%' or age_ like '%%%s%%')", searchText, searchText);
        }
        log.debug("sql {}", query.getSqlText().getText());

        // 将准备好的sql语句执行，并将结果存放于cdsTmp对象。
        query.open();
        // 将sql查询出来的结果存放到服务出口返回给调用者
        getDataOut().appendDataSet(query);
        // 返回meta讯息
        FieldDefs columns = getDataOut().getFieldDefs();
        columns.get("code_").setName("工号");
        columns.get("name_").setName("姓名");
        columns.get("sex_").setName("性别");
        columns.get("age_").setName("年龄");
        columns.get("createTime_").setName("创建时间");
        getDataOut().setMetaInfo(true);
        return true;
    }
}
