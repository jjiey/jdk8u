package yjtest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author yangsanity
 */
public class AbstractQueuedSynchronizerTest {

    public static void main(String[] args) {
        DebugTest4 d = new DebugTest4();
        // d.acquire(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
//        executor.execute(() -> d.acquire(1));
        // test acquireQueued
//        executor.execute(() -> {
//            // 保证该线程后获取锁
//            try {
//                Thread.sleep(100L);
//            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            d.acquire(1);
//        });

        executor.execute(() -> d.acquire(1));
        executor.execute(() -> d.acquire(1));
        executor.execute(() -> {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            d.release(1);
        });
    }

    static class DebugTest extends AbstractQueuedSynchronizer {

        @Override
        protected boolean tryAcquire(int arg) {
            // test acquireQueued
            return false;
        }
    }

    static class DebugTest2 extends AbstractQueuedSynchronizer {

        private AtomicInteger integer = new AtomicInteger(0);

        @Override
        protected boolean tryAcquire(int arg) {
            // test acquireQueued finally
            if (integer.incrementAndGet() == 2) {
                int i = 1 / 0;
            }
            return false;
        }
    }

    static class DebugTest3 extends AbstractQueuedSynchronizer {

        private AtomicInteger integer = new AtomicInteger(0);

        @Override
        protected boolean tryAcquire(int arg) {
            // test setHead
            if (integer.incrementAndGet() == 2) {
                return true;
            }
            return false;
        }
    }

    static class DebugTest4 extends AbstractQueuedSynchronizer {

        private AtomicInteger integer = new AtomicInteger(0);

        @Override
        protected boolean tryAcquire(int arg) {
            // test setHead
            if (integer.incrementAndGet() == 5) {
                int i = 1 / 0;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            return true;
        }
    }

    /**
     *
     */
    class Mutex implements Lock, java.io.Serializable {
        // Our internal helper class
        // 内部帮助类
        private class Sync extends AbstractQueuedSynchronizer {
            // Reports whether in locked state
            // 报告是否处于锁定状态
            @Override
            protected boolean isHeldExclusively() {
                return getState() == 1;
            }

            // Acquires the lock if state is zero
            // 如果状态为零，则获取锁
            @Override
            public boolean tryAcquire(int acquires) {
                assert acquires == 1; // Otherwise unused 否则未使用
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            // Releases the lock by setting state to zero
            // 通过将状态设置为零来释放锁
            @Override
            protected boolean tryRelease(int releases) {
                assert releases == 1; // Otherwise unused 否则未使用
                if (getState() == 0) {
                    throw new IllegalMonitorStateException();
                }
                setExclusiveOwnerThread(null);
                setState(0);
                return true;
            }

            // Provides a Condition
            // 提供一个 Condition
            Condition newCondition() {
                return new ConditionObject();
            }

            // Deserializes properly
            // 正确反序列化
            private void readObject(ObjectInputStream s)
                    throws IOException, ClassNotFoundException {
                s.defaultReadObject();
                setState(0); // reset to unlocked state 重置为 unlocked 状态
            }
        }

        // The sync object does all the hard work. We just forward to it.
        // 同步对象完成了所有困难的工作。我们只是直接用它。
        private final Sync sync = new Sync();

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }

        public boolean isLocked() {
            return sync.isHeldExclusively();
        }

        public boolean hasQueuedThreads() {
            return sync.hasQueuedThreads();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }
    }

    /**
     *
     */
    static class BooleanLatch {
        private class Sync extends AbstractQueuedSynchronizer {
            boolean isSignalled() {
                return getState() != 0;
            }

            @Override
            protected int tryAcquireShared(int ignore) {
                return isSignalled() ? 1 : -1;
            }

            @Override
            protected boolean tryReleaseShared(int ignore) {
                setState(1);
                return true;
            }
        }

        private final Sync sync = new Sync();

        public boolean isSignalled() {
            return sync.isSignalled();
        }

        public void signal() {
            sync.releaseShared(1);
        }

        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
    }
}
