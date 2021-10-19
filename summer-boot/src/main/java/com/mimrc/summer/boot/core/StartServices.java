package com.mimrc.summer.boot.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.cerc.core.DataSet;
import cn.cerc.db.other.RecordFilter;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.BasicHandle;
import cn.cerc.mis.core.IService;

@RestController
@RequestMapping(value = "/services", produces = "application/json;charset=utf-8")
public class StartServices {

    @Autowired
    ApplicationContext context;

    @RequestMapping("/")
    String index() {
        return new DataSet().setMessage("service is null").toJson();
    }

    @RequestMapping("/{service}.{method}")
    String services(@PathVariable String service, @PathVariable String method,
            @RequestParam(required = false) String sid,
            @RequestParam(required = false, defaultValue = "{}") String dataIn) {
        Application.setContext(context);
        try (BasicHandle handle = new BasicHandle()) {
            try {
                IService bean = Application.getService(handle, service + "." + method);
                DataSet params = new DataSet().fromJson(dataIn);
                DataSet dataOut = bean.execute(handle, params);
                return RecordFilter.execute(dataOut, params.getHead().getString("_RecordFilter_")).toJson();
            } catch (Exception e) {
                e.printStackTrace();
                return new DataSet().setMessage(e.getMessage()).toJson();
            }
        }
    }
}
