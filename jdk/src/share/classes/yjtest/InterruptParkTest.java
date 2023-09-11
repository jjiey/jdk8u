package yjtest;

import java.util.concurrent.locks.LockSupport;

public class InterruptParkTest {

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Test3();
        t.start();
        // for test4
        t.interrupt();
    }

    /**
     * 程序正常执行结束，说明：一次中断操作后，无论线程调用多少次 LockSupport.park()，程序都不会挂起，而是正常运行结束
     */
    public static class Test1 extends Thread {

        @Override
        public void run() {
            Thread.currentThread().interrupt();
            LockSupport.park();
            System.out.println("第一次 park() 后");
            LockSupport.park();
            System.out.println("第二次 park() 后");
            LockSupport.park();
            System.out.println("第三次 park() 后");
        }
    }

    /**
     * 输出第一条打印语句后挂起，说明：无论调用多少次 LockSupport.unpark(Thread.currentThread())，只会提供给线程一个许可
     */
    public static class Test2 extends Thread {

        @Override
        public void run() {
            LockSupport.unpark(Thread.currentThread());
            LockSupport.unpark(Thread.currentThread());
            LockSupport.unpark(Thread.currentThread());
            LockSupport.park();
            System.out.println("第一次 park() 后");
            LockSupport.park();
            System.out.println("第二次 park() 后");
        }
    }

    /**
     * 第二次 LockSupport.park() 挂起，结合 test1，说明：一次中断操作后，无论标志位是否重置，第一次 park 不会挂起，第二次 park 会挂起
     * AQS
     */
    public static class Test3 extends Thread {

        @Override
        public void run() {
            b();
        }

        private void a() {
            Thread.currentThread().interrupt();
            LockSupport.park();
            System.out.println("第一次 park() 后");
            System.out.println(Thread.interrupted()); // true
            System.out.println(Thread.interrupted()); // false
            LockSupport.park(); // 挂起
            System.out.println("第二次 park() 后");
        }

        private void b() {
            Thread.currentThread().interrupt();
            System.out.println(Thread.interrupted()); // true
            System.out.println(Thread.interrupted()); // false
            LockSupport.park();
            System.out.println("第一次 park() 后");
            LockSupport.park(); // 挂起
            System.out.println("第二次 park() 后");
        }
    }

    /**
     * 第一个响应的是 sleep 而不是 park，第二个 LockSupport.park() 挂起，说明：标志位重置后，第一次 park 不会挂起，第二次 park 会挂起
     */
    public static class Test4 extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("第一次 sleep() 后");
            System.out.println(Thread.interrupted()); // false
            System.out.println(Thread.interrupted()); // false
            LockSupport.park();
            System.out.println("第二次 park() 后");
            LockSupport.park(); // 挂起
            System.out.println("第三次 park() 后");
        }
    }
}
