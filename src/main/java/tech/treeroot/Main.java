package tech.treeroot;

import tech.treeroot.timer.Timer;
import tech.treeroot.timer.impl.TimerImpl;
import tech.treeroot.timer.TimerTask;

public class Main {

    public static void main(String[] args) {
        Timer timer = new TimerImpl(1000, 10, 5000);
        timer.add(new DemoTimerTask(2000));
        timer.add(new DemoTimerTask(2000));
        timer.add(new DemoTimerTask(5000));
        timer.add(new DemoTimerTask(5000));
        timer.add(new DemoTimerTask(15000));
        timer.add(new DemoTimerTask(15000));
        timer.add(new DemoTimerTask(30000));
        timer.add(new DemoTimerTask(30000));
    }

    private static class DemoTimerTask extends TimerTask {

        private String name;

        public DemoTimerTask(long delayTime) {
            super(delayTime);
            this.name = delayTime + "";
        }

        @Override
        public void run() {
            System.out.println(name + " 执行了");
        }
    }

}
