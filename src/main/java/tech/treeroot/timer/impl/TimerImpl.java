package tech.treeroot.timer.impl;

import tech.treeroot.timer.*;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TimerImpl implements Timer {

    private ExecutorService taskExecutor;
    private ExecutorService bossExecutor;
    private DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();
    private AtomicInteger taskCounter = new AtomicInteger();
    private TimingWheel timingWheel;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();

    public TimerImpl(long tickMs, int wheelSize, long pollInterval) {
        timingWheel = new TimingWheel(tickMs, wheelSize, System.currentTimeMillis(), taskCounter, delayQueue);
        taskExecutor = Executors.newFixedThreadPool(100);
        bossExecutor = Executors.newFixedThreadPool(1);
        bossExecutor.submit((Runnable) () -> {
            while (true) {
                try {
                    advanceClock(pollInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void add(TimerTask task) {
        readLock.lock();
        try {
            addTimerTaskEntry(new TimerTaskEntry(task, task.getDelayTime() + System.currentTimeMillis()));
        } finally {
            readLock.unlock();
        }
    }

    private void addTimerTaskEntry(TimerTaskEntry timerTaskEntry) {
        if (!timingWheel.add(timerTaskEntry)) {
            if (!timerTaskEntry.cancelled()) {
                taskExecutor.submit(timerTaskEntry.getTimerTask());
            }
        }
    }

    @Override
    public boolean advanceClock(long timeout) throws InterruptedException {
        TimerTaskList bucket = delayQueue.poll(timeout, TimeUnit.MILLISECONDS);
        boolean res;
        if (bucket != null) {
            try {
                writeLock.lock();
                while (bucket != null) {
                    timingWheel.advanceClock(bucket.getExpiration());
                    bucket.flush(this::addTimerTaskEntry);
                    bucket = delayQueue.poll();
                }
            } finally {
                writeLock.unlock();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int size() {
        return taskCounter.get();
    }

    @Override
    public void shutdown() {
        taskExecutor.shutdown();
        bossExecutor.shutdown();
    }
}
