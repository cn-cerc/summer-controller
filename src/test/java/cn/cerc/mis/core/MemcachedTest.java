package cn.cerc.mis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.db.redis.Redis;

public class MemcachedTest {
    private static final Logger log = LoggerFactory.getLogger(MemcachedTest.class);

    @Test
    @Ignore
    public void test() {
        String buffKey = "test";
        String value = "OK!";
        Redis.setValue(buffKey, value, 2);

        Object buffData;
        for (int i = 1; i < 5; i++) {
            buffData = Redis.getValue(buffKey);
            String msg = String.format("第 %d 次测试", i);
            assertEquals(msg, i <= 2 ? value : null, buffData);
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1005);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
