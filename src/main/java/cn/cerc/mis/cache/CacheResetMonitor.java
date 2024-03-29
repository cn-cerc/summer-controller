package cn.cerc.mis.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.cerc.db.redis.Redis;

public class CacheResetMonitor extends Thread {
    private final SubCacheEvent monitor = new SubCacheEvent();
    private final AtomicBoolean isStop = new AtomicBoolean(false);
    private final Redis redis = new Redis();

    @Override
    public void run() {
        // 此方法是线程阻塞的，所以按照原来的 try-resources 的逻辑也同样是独占一个Redis连接
        redis.subscribe(monitor, MemoryListener.CacheChannel);
    }

    public void requestStop() {
        // 经过测试 monitor.isSubscribed() 方法的状态更新不及时，会导致重复执行 monitor.unsubscribe() 引起报错
        if (isStop.get())
            return;
        synchronized (this) {
            if (isStop.get())
                return;
            monitor.unsubscribe();
            isStop.set(true);
        }
    }

}