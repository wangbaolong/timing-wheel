package tech.treeroot.timer;

public interface Timer {

    /**
     * 增加一个延迟任务
     * @param task
     */
    void add(TimerTask task);

    /**
     * 推荐时间轮
     * @param timeout
     * @return
     * @throws InterruptedException
     */
    boolean advanceClock(long timeout) throws InterruptedException;

    /**
     * @return  当前任务数量
     */
    int size();

    /**
     * 关闭时间轮
     */
    void shutdown();

}
