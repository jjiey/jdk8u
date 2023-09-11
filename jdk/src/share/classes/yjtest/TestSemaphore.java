package yjtest;

import java.util.concurrent.Semaphore;

/**
 * @author yangsanity
 */
public class TestSemaphore {

    /**
     * Semaphore 初始状态为 0
     */
    private static final Semaphore SEM = new Semaphore(0);

    private static class Thread1 extends Thread {
        @Override
        public void run() {
            // 获取 1 个许可，会阻塞等待其他线程释放许可，可被中断
            SEM.acquireUninterruptibly();
        }
    }

    private static class Thread2 extends Thread {
        @Override
        public void run() {
            // 释放 1 个许可
            SEM.release();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10000000; i++) {
            Thread t1 = new Thread1();
            Thread t2 = new Thread1();
            Thread t3 = new Thread2();
            Thread t4 = new Thread2();
            t1.start();
            t2.start();
            t3.start();
            t4.start();
            t1.join();
            t2.join();
            t3.join();
            t4.join();
            System.out.println(i);
        }
    }
}
