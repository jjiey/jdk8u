package yjtest;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * @author yangsanity
 */
public class LockSupportTest {

    public static void main(String[] args) {

    }

    /**
     * 先进先出非重入锁类 demo
     * from {@link LockSupport}
     */
    class FIFOMutex {

        private final AtomicBoolean locked = new AtomicBoolean(false);

        private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

        public void lock() {
            boolean wasInterrupted = false;
            Thread current = Thread.currentThread();
            waiters.add(current);
            // 不是队列中的第一个或无法获取锁时阻塞
            while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
                LockSupport.park(this);
                // 等待时忽略中断
                if (Thread.interrupted()) {
                    wasInterrupted = true;
                }
            }
            waiters.remove();
            // 退出时 reassert 中断状态
            if (wasInterrupted) {
                current.interrupt();
            }
        }

        public void unlock() {
            locked.set(false);
            LockSupport.unpark(waiters.peek());
        }
    }

    static class ParkTest {

        private final Object blocker = new Object();

        Thread park = new Thread(() -> {
            System.out.println("进入阻塞休眠");
            LockSupport.park(blocker); // 1
            System.out.println("退出阻塞休眠");
        });

        Thread unpark = new Thread(() -> {
            // 保证 park 线程进入休眠
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 唤醒 park 线程
            LockSupport.unpark(park); // 2
        });

        public static void main(String[] args) {
            ParkTest t = new ParkTest();
            t.park.start();
            t.unpark.start();
        }
    }

    /**
     * 提前调用了 unpark(thread)，目标线程调用 park 时不会进入休眠，而是直接被唤醒
     */
    static class ParkTest2 {

        private final Object blocker = new Object();

        Thread park = new Thread(() -> {
            // 保证 unpark 线程提前唤醒
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("进入阻塞休眠");
            LockSupport.park(blocker); // 1
            System.out.println("退出阻塞休眠");
        });

        Thread unpark = new Thread(() -> {
            // 唤醒 park 线程
            LockSupport.unpark(park); // 2
        });

        public static void main(String[] args) {
            ParkTest2 t = new ParkTest2();
            t.park.start();
            t.unpark.start();
        }
    }
}
