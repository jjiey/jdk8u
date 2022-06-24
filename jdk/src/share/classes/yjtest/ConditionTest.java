package yjtest;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yangsanity
 */
public class ConditionTest {

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            int item = 0;
            while (true) {
                try {
                    // 放得快
                    buffer.put(item);
                    Thread.sleep(600L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                item++;
            }
        });
        executor.execute(() -> {
            while (true) {
                try {
                    // 取的慢
                    buffer.take();
                    Thread.sleep(1000L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static class BoundedBuffer<T> {
        private static final Lock lock = new ReentrantLock();
        // 非满 / 非空（items 数组状态）
        private static final Condition notFull = lock.newCondition(), notEmpty = lock.newCondition();

        private final Object[] items;
        private int putptr, takeptr, count;

        BoundedBuffer(int size) {
            items = new Object[size];
        }

        public void put(T x) throws InterruptedException {
            lock.lock();
            try {
                // buffer 满
                while (count == items.length) {
                    System.out.println("buffer is full...");
                    notFull.await();
                }
                // 入队
                items[putptr] = x;
                // 如果 putptr 到头了，重置 putptr（有点循环的意思）
                if (++putptr == items.length) {
                    putptr = 0;
                }
                // put 后总数递增
                ++count;
                System.out.println("有元素，可以来取: put " + x);
                notEmpty.signal();
            }
            finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lock();
            try {
                // buffer 空
                while (count == 0) {
                    System.out.println("buffer is empty...");
                    notEmpty.await();
                }
                // 出队
                Object x = items[takeptr];
                // 如果 takeptr 到头了，重置 takeptr（有点循环的意思）
                if (++takeptr == items.length) {
                    takeptr = 0;
                }
                // take 后总数递减
                --count;
                System.out.println("有空间，可以往里放: take " + x);
                notFull.signal();
                return (T) x;
            }
            finally {
                lock.unlock();
            }
        }
    }

    /**
     * awaitNanos 和 awaitUntil 用法示例
     */
    static class AwaitNanosAndAwaitUntilDemo {

        private static final Lock lock = new ReentrantLock();
        private static final Condition condition = lock.newCondition();

        boolean aMethod(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            lock.lock();
            try {
                while (!conditionBeingWaitedFor()) {
                    if (nanos <= 0L) {
                        return false;
                    }
                    nanos = condition.awaitNanos(nanos);
                }
                // ...
            } finally {
                lock.unlock();
            }
            // return false directly
            return false;
        }

        boolean awaitUntil(Date deadline) throws InterruptedException {
            boolean stillWaiting = true;
            lock.lock();
            try {
                while (!conditionBeingWaitedFor()) {
                    if (!stillWaiting) {
                        return false;
                    }
                    stillWaiting = condition.awaitUntil(deadline);
                }
                // ...
            } finally {
                lock.unlock();
            }
            // return false directly
            return false;
        }

        boolean conditionBeingWaitedFor() {
            // return false directly
            return false;
        }
    }
}
