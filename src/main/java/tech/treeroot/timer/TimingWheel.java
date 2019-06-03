package tech.treeroot.timer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TimingWheel {

    private long tickMs;
    private int wheelSize;
    private long interval;
    private long startMs;
    private AtomicInteger taskCounter;
    private DelayQueue queue;
    private long currentTime;
    private TimerTaskList[] buckets;

    private volatile TimingWheel overflowWheel;

    /**
     * @param tickMs 轮子每个格子的时间
     * @param wheelSize 每个轮子的大小
     * @param startMs 开始时间
     * @param taskCounter 任务计数器
     * @param queue 延迟队列
     */
    public TimingWheel(long tickMs, int wheelSize, long startMs, AtomicInteger taskCounter, DelayQueue queue) {
        this.tickMs = tickMs;
        this.wheelSize = wheelSize;
        this.interval = tickMs * wheelSize;
        this.startMs = startMs;
        this.taskCounter = taskCounter;
        this.queue = queue;
        this.currentTime = startMs - (startMs % tickMs);
        buckets = new TimerTaskList[wheelSize];
        for (int i = 0; i < wheelSize; i++) {
            buckets[i] = new TimerTaskList(taskCounter);
        }

    }

    /**
     * 增加一个上层时间轮
     */
    private void addOverflowWheel() {
        synchronized (this) {
            if (overflowWheel == null) {
                overflowWheel = new TimingWheel(interval, wheelSize, startMs, taskCounter, queue);
            }
        }
    }

    /**
     * 添加任务
     * @param timerTaskEntry
     * @return
     */
    public boolean add(TimerTaskEntry timerTaskEntry) {
        long expiration = timerTaskEntry.expirationMs;
        boolean res = false;
        if (timerTaskEntry.cancelled()) {
            return false;
        } else if (expiration < currentTime + tickMs) {
            return false;
        } else if (expiration < currentTime + interval) {
            long virtualId = expiration / tickMs;
            int index = (int) (virtualId % wheelSize);
            TimerTaskList bucket = buckets[index];
            bucket.add(timerTaskEntry);
            if (bucket.setExpiration(virtualId * tickMs)) {
                queue.offer(bucket);
            }
            return true;
        } else {
            if (overflowWheel == null) {
                addOverflowWheel();
            }
            overflowWheel.add(timerTaskEntry);
            return true;
        }
    }

    /**
     * 时间推进
     * @param timeMs
     */
    public void advanceClock(long timeMs) {
        if (timeMs >= currentTime + tickMs) {
            currentTime = timeMs - (timeMs % tickMs);
            // Try to advance the clock of the overflow wheel if present
            if (overflowWheel != null) {
                overflowWheel.advanceClock(currentTime);
            }
        }
    }

}
