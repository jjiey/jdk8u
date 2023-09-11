package yjtest;

import java.util.concurrent.locks.ReentrantLock;

public class TestAQSHeadIsNullAfterInit {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        TestAQSHeadIsNullAfterInit test = new TestAQSHeadIsNullAfterInit();
        Thread t1 = new Thread(() -> test.run(lock));
        Thread t2 = new Thread(() -> test.run(lock));
        t1.start();
        t2.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(1);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(1);
    }

    public void run(ReentrantLock lock) {
        lock.lock();
        try {
            System.out.println("===>" + Thread.currentThread().getName());
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
