package yjtest;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * @author yangsanity
 */
public class ExecutorTest {

    public static void main(String[] args) {

    }

    /**
     * 一个复合的 executor：串行化将任务提交给第二个 executor
     */
    class SerialExecutor implements Executor {

        final Queue<Runnable> tasks = new ArrayDeque<>();
        final Executor executor;
        Runnable active;

        SerialExecutor(Executor executor) {
            this.executor = executor;
        }

        public synchronized void execute(final Runnable r) {
            // 往 queue 里扔个 Runnable，该 Runnable 执行完成后会调用 scheduleNext()
            tasks.offer(() -> {
                try {
                    r.run();
                }
                finally {
                    scheduleNext();
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                executor.execute(active);
            }
        }
    }
}
