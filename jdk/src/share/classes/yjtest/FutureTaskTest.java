package yjtest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yangsanity
 */
public class FutureTaskTest {

    public static void main(String[] args) throws Exception {
        // 首先我们创建了一个线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        FutureTask<String> futureTask = new FutureTask<>(() -> {
            Thread.sleep(3000);
            // 返回一句话
            return "我是子线程 " + Thread.currentThread().getName();
        });

        // 把任务提交到线程池中，线程池会分配线程帮我们执行任务
        executor.submit(futureTask);
//        // 得到任务执行的结果
//        String result = futureTask.get();
//        System.out.println("result is " + result);
        new TestWaitNode(futureTask, "thread1").start();
        new TestWaitNode(futureTask, "thread2").start();
        new TestWaitNode(futureTask, "thread3").start();
    }

    /**
     * for test FutureTask.WaitNode
     */
    static class TestWaitNode extends Thread {

        private final FutureTask<String> futureTask;

        TestWaitNode(FutureTask<String> futureTask, String name) {
            this.futureTask = futureTask;
            this.setName(name);
        }

        @Override
        public void run() {
            try {
                String s = futureTask.get();
                System.out.println("Thread [" + Thread.currentThread().getName() + "] get result [" + s + "]");
            }
            catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
