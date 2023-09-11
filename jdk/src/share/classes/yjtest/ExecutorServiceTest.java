package yjtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yangsanity
 */
public class ExecutorServiceTest {

    public static void main(String[] args) {

    }

    class NetworkService implements Runnable {
        private final ServerSocket serverSocket;
        private final ExecutorService pool;

        public NetworkService(int port, int poolSize) throws IOException {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    pool.execute(new Handler(serverSocket.accept()));
                }
            }
            catch (IOException ex) {
                pool.shutdown();
            }
        }
    }

    class Handler implements Runnable {
        private final Socket socket;

        Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // read and service request on socket
        }
    }

    /**
     * 两阶段关闭 ExecutorService
     */
    void shutdownAndAwaitTermination(ExecutorService pool) {
        // 禁止提交新任务
        pool.shutdown();
        try {
            // 等待现有的任务终止
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                // 取消当前正在执行的任务
                pool.shutdownNow();
                // 等待任务响应被取消
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        }
        catch (InterruptedException ie) {
            // 如果当前线程也被中断，则（重新）取消
            pool.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
    }
}
