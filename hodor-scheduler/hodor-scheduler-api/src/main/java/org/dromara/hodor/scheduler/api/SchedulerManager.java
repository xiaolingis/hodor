package org.dromara.hodor.scheduler.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.dromara.hodor.common.extension.ExtensionLoader;
import org.dromara.hodor.scheduler.api.config.SchedulerConfig;

/**
 * @author tangzy
 * @since 1.0
 */
public final class SchedulerManager {

    public static final SchedulerManager INSTANCE = new SchedulerManager();

    private final ReentrantLock lock;
    private final Map<String, HodorScheduler> activeSchedulerMap;
    private final Map<String, HodorScheduler> standBySchedulerMap;
    private final ExtensionLoader<HodorScheduler> extensionLoader;

    private SchedulerManager() {
        lock = new ReentrantLock();
        activeSchedulerMap = new ConcurrentHashMap<>();
        standBySchedulerMap = new ConcurrentHashMap<>();
        extensionLoader = ExtensionLoader.getExtensionLoader(HodorScheduler.class, SchedulerConfig.class);
    }

    public HodorScheduler createScheduler(SchedulerConfig config) {
        return extensionLoader.getProtoJoin("scheduler", config);
    }

    public HodorScheduler createScheduler(String schedulerName, SchedulerConfig config) {
        return extensionLoader.getProtoJoin(schedulerName, config);
    }

    public void addActiveScheduler(HodorScheduler scheduler) {
        lock.lock();
        try {
            standBySchedulerMap.remove(scheduler.getSchedulerName());
            activeSchedulerMap.putIfAbsent(scheduler.getSchedulerName(), scheduler);
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void addStandByScheduler(HodorScheduler scheduler) {
        lock.lock();
        try {
            activeSchedulerMap.remove(scheduler.getSchedulerName());
            standBySchedulerMap.putIfAbsent(scheduler.getSchedulerName(), scheduler);
            if (scheduler.isStarted()) {
                scheduler.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }

}
