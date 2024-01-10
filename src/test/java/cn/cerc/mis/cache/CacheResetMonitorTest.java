package cn.cerc.mis.cache;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheResetMonitorTest {

    private static final Logger log = LoggerFactory.getLogger(CacheResetMonitorTest.class);

    @Test
    @Ignore
    public void testRequestStop() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CacheResetMonitor monitor1 = new CacheResetMonitor();
        Future<?> future = pool.submit(monitor1);
        pool.shutdown();
        Thread.sleep(1000);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("requestStop");
                monitor1.requestStop();
            }).start();
        }
        countDownLatch.countDown();
        log.info("countDownLatch.countDown");
        future.get();
    }

}