package yjtest;

/**
 * @author yangsanity
 */
public class ThreadTest {

    public static void main(String[] args) throws Exception {
        ThreadTest t = new ThreadTest();
        // t.testJoin();
        t.testPriority();
        // t.testInterrupt();
        // t.testInterrupt2();
        // t.testInterrupt3();
        // t.testInterrupt4();
    }

    /**
     * test join
     */
    private void testJoin() throws Exception {
        System.out.println("main Thread {" + Thread.currentThread().getName() + "} begin run");
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            try {
                Thread.sleep(5000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 抛异常就不再往下走了，也属于线程结束
            // int i = 1 / 0;
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        thread.start();
        Thread.sleep(100L);
        // 主线程等待子线程执行完成之后再执行
        System.out.println("{" + Thread.currentThread().getName() + "} is waiting for {" + thread.getName() + "} to die...");
        thread.join();
        System.out.println("main Thread {" + Thread.currentThread().getName() + "} end run");
    }

    /**
     * test priority
     */
    private void testPriority() {
        System.out.println("main Thread {" + Thread.currentThread().getName() + "} begin run");
        // new Thread 之前设置
        Thread.currentThread().setPriority(3);
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        // Thread.currentThread().setPriority(3);
        thread.start();
        System.out.println(Thread.currentThread().getPriority());
        System.out.println(thread.getPriority());
    }

    /**
     * test interrupt
     */
    public void testInterrupt() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            try {
                System.out.println("new Thread {" + Thread.currentThread().getName() + "} sleep 10s");
                // sleep or wait
                Thread.sleep(10000L);
            }
            catch (InterruptedException e) {
                System.out.println("new Thread {" + Thread.currentThread().getName() + "} is interrupted");
                e.printStackTrace();
            }
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        thread.start();
        Thread.sleep(5000L);
        System.out.println("After the main Thread {" + Thread.currentThread().getName() + "} waited for 5s, new Thread {" + thread.getName()
                + "} had not run successfully, interrupt it");
        thread.interrupt();
    }

    /**
     * test interrupt：通过捕获中断异常响应中断
     */
    public void testInterrupt2() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            while (true) {
                try {
                    System.out.println("new Thread {" + Thread.currentThread().getName() + "} sleep 2s, isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");
                    // sleep or wait
                    Thread.sleep(2000L);
                }
                catch (InterruptedException e) {
                    System.out.println(
                            "new Thread {" + Thread.currentThread().getName() + "} is interrupted, isInterrupted: [" + Thread.currentThread().isInterrupted() + "]...");
                    if (Thread.currentThread().isInterrupted()) {
                        // Unreachable statement, because 抛出 InterruptedException 异常时，当前线程的 中断状态 会被清除
                        System.out.println("isInterrupted is true, new Thread {" + Thread.currentThread().getName() + "}'s interrupted status is cleared" + "...");
                        Thread.interrupted();
                    }
                    e.printStackTrace();
                    // break or not
                    // break;
                }
            }
            // Unreachable statement
            // System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        thread.start();
        Thread.sleep(7000L);
        System.out.println("main Thread {" + Thread.currentThread().getName() + "} interrupt new Thread {" + thread.getName() + "}");
        thread.interrupt();
    }

    /**
     * test interrupt：通过检查中断位响应中断，重置 中断状态
     */
    public void testInterrupt3() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("new Thread {" + Thread.currentThread().getName() + "}'s isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");
            }
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("isInterrupted is [true], new Thread {" + Thread.currentThread().getName() + "}'s interrupted status is cleared" + "...");
                Thread.interrupted();
            }
            System.out.println("===> new Thread {" + Thread.currentThread().getName() + "}'s isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        thread.start();
        Thread.sleep(1L);
        thread.interrupt();
    }

    /**
     * test interrupt：通过检查中断位响应中断，不重置 中断状态，直接 sleep
     */
    public void testInterrupt4() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} begin run");
            while (!Thread.currentThread().isInterrupted()) {
                // System.out.println("new Thread {" + Thread.currentThread().getName() + "}'s isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");
            }
            System.out.println("===> new Thread {" + Thread.currentThread().getName() + "}'s isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");

            try {
                System.out.println("new Thread {" + Thread.currentThread().getName() + "} sleep 2s");
                // sleep or wait
                // 无需打断，直接抛出异常
                Thread.sleep(2000L);
                System.out.println("reachable ???");
            }
            catch (InterruptedException e) {
                System.out.println("new Thread {" + Thread.currentThread().getName() + "} is interrupted");
                e.printStackTrace();
            }

            System.out.println("===> new Thread {" + Thread.currentThread().getName() + "}'s isInterrupted: [" + Thread.currentThread().isInterrupted() + "]");
            System.out.println("new Thread {" + Thread.currentThread().getName() + "} end run");
        });
        thread.start();
        Thread.sleep(1000L);
        thread.interrupt();
    }
}
