package cn.cerc.mis.ado;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import cn.cerc.db.core.SqlText;
import cn.cerc.db.redis.JedisFactory;
import redis.clients.jedis.Jedis;

public class EntityHomeTest {
    private static final Logger log = LoggerFactory.getLogger(EntityHomeTest.class);

    @Test
    public void test() {
        String json = """
                {"UID_":14077,"CorpCode_":"EasyERP","CorpNo_":"220701","Code_":"0pv9647e25","ID_":"7265c36716c74ee7a055371d62c0bc29","Name_":"葛祥迪","RoleCode_":"WL-SJ1-002","DiyRole_":false,"DeptCode_":"","Enabled_":1,"Lock_":false,"Password_":"cdb97398831f7f922484904428e9c4fa","ShowInUP_":0,"ShowOutUP_":0,"ShowListUP_":0,"ShowOutUP2_":0,"ShowAllCus_":true,"Finger_":null,"EmailAccount_":null,"EmailPassword_":null,"EmailAddress_":null,"EmailPOP3_":null,"EmailSMTP_":null,"EmailSubject_":null,"TimeLock_":null,"TimeIn1_":null,"TimeOut1_":null,"TimeIn2_":null,"TimeOut2_":null,"TimeIn3_":null,"TimeOut3_":null,"Web_":null,"Out_":false,"FinalSubject_":null,"Encrypt_":true,"Title_":null,"Remark_":null,"RemarkID_":null,"Agent_":null,"PersonCode_":null,"AutoExitTime_":0,"Mobile_":"15154720202","QQ_":"","SMSNo_":null,"ShareAccount_":false,"SecurityLevel_":0,"SecurityMachine_":null,"PCMachine1_":null,"PCMachine2_":null,"PCMachine3_":null,"ERPAccount_":"","ERPPassword_":"","SuperUser_":false,"UpdateKey_":"{8fa31f1a-f67c-4b1b-8a51-3e1c1159fc0f}","ProxyUsers_":"","FlowProxy_":"","LastRemindDate_":null,"BelongAccount_":"","BelongCorpCode_":"","Salesman_":false,"ImageUrl_":"","AccountType_":0,"VerifyTimes_":0,"LastCorpNo_":""}
                """;

        // 准备键列表和值列表
        List<String> batchKeys = new ArrayList<>();
        List<String> batchValues = new ArrayList<>();
        for (int i = 0; i < SqlText.MAX_RECORDS; i++) {
            batchKeys.add(String.valueOf(i));
            batchValues.add(json);
            batchValues.add("3600");
        }

        StopWatch stop = new StopWatch();
        stop.start();
        try (Jedis jedis = JedisFactory.getJedis()) {
            // 执行 Lua 脚本
            Long result = (Long) jedis.eval(EntityHome.luaScript, batchKeys, batchValues);
            log.info("写入总数 {}", result);
            jedis.close();
        }
        stop.stop();
        log.info("写入耗时 {} ms", stop.getTotalTimeMillis());// 1305ms
    }
}
