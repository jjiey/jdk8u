package yjtest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yangsanity
 */
public class ThreadPoolExecutorTest {

    private static final int COUNT_BITS = Integer.SIZE - 3;
    // 00000000 00000000 00000000 00000001 -> 00100000 00000000 00000000 00000000 = 2 ^ 29
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;
    // -1 补码: 11111111 11111111 11111111 11111111 -> 11100000 00000000 00000000 00000000 -> 11011111 11111111 11111111 11111111 -> 10100000 00000000 00000000 00000000 = - 2 ^ 29
    private static final int RUNNING = -1 << COUNT_BITS;
    // 0
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    // 2 ^ 29
    private static final int STOP = 1 << COUNT_BITS;
    // 00000000 00000000 00000000 00000010 -> 01000000 00000000 00000000 00000000 = 2 ^ 30
    private static final int TIDYING = 2 << COUNT_BITS;
    // 00000000 00000000 00000000 00000011 -> 01100000 00000000 00000000 00000011 = 2 ^ 30 + 2 ^ 29
    private static final int TERMINATED = 3 << COUNT_BITS;

    public static void main(String[] args) {
        System.out.println("COUNT_BITS: " + COUNT_BITS);
        System.out.println("CAPACITY: " + CAPACITY);
        System.out.println("RUNNING: " + RUNNING);
        System.out.println("SHUTDOWN: " + SHUTDOWN);
        System.out.println("STOP: " + STOP);
        System.out.println("TIDYING: " + TIDYING);
        System.out.println("TERMINATED: " + TERMINATED);
        // 与运算 &: 同 1 为 1
        // 非运算 ~: 每位取反
        // 或运算 |: 只要有一个为 1，结果就是 1
        int ctl = ctlOf(RUNNING, 0);
        System.out.println("ctl: " + ctl);
        System.out.println("runState: " + runStateOf(ctl));
        System.out.println("workerCount: " + workerCountOf(ctl));
    }

    private static int runStateOf(int c) {
        // 00100000 00000000 00000000 00000000
        // 11011111 11111111 11111111 11111111
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        // 00100000 00000000 00000000 00000000
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    // ========= //

    /**
     * 添加简单 暂停/恢复 功能
     */
    class PausableThreadPoolExecutor extends ThreadPoolExecutor {
        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                          BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                          RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) {
                    unpaused.await();
                }
            }
            catch (InterruptedException ie) {
                t.interrupt();
            }
            finally {
                pauseLock.unlock();
            }
        }

        /**
         * 暂停
         */
        public void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            }
            finally {
                pauseLock.unlock();
            }
        }

        /**
         * 恢复
         */
        public void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            }
            finally {
                pauseLock.unlock();
            }
        }
    }
}
