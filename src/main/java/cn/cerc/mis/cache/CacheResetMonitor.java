package cn.cerc.mis.cache;

import cn.cerc.db.redis.Redis;

public class CacheResetMonitor extends Thread {
    private SubCacheEvent monitor = new SubCacheEvent();

    @Override
    public void run() {
        try (Redis jedis = new Redis()) {
            if (jedis != null)
                jedis.subscribe(monitor, MemoryListener.CacheChannel);
        }
    }

    public void requestStop() {
        if (monitor.isSubscribed())
            monitor.unsubscribe();
    }
}