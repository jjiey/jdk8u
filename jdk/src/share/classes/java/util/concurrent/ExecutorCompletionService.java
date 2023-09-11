/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A {@link CompletionService} that uses a supplied {@link Executor}
 * to execute tasks.  This class arranges that submitted tasks are,
 * upon completion, placed on a queue accessible using {@code take}.
 * The class is lightweight enough to be suitable for transient use
 * when processing groups of tasks.
 * 一个使用提供的 Executor 执行任务的 CompletionService。此类安排将提交的任务在完成后放置在使用 take 可访问的队列中。该类足够轻量，适合在处理任务组时临时使用。
 *
 * <p>
 *
 * <b>Usage Examples.</b>
 * 翻译：使用示例
 *
 * Suppose you have a set of solvers for a certain problem, each
 * returning a value of some type {@code Result}, and would like to
 * run them concurrently, processing the results of each of them that
 * return a non-null value, in some method {@code use(Result r)}. You
 * could write this as:
 * 翻译：假设你有一组特定问题的 solvers，每个 solver 都返回某种类型的值 Result，并且希望并发运行它们，在方法 use(Result r) 中处理它们中每个返回非空值的结果。你可以这样写：
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException, ExecutionException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     for (Callable<Result> s : solvers)
 *         ecs.submit(s);
 *     int n = solvers.size();
 *     for (int i = 0; i < n; ++i) {
 *         Result r = ecs.take().get();
 *         if (r != null)
 *             use(r);
 *     }
 * }}</pre>
 *
 * Suppose instead that you would like to use the first non-null result
 * of the set of tasks, ignoring any that encounter exceptions,
 * and cancelling all other tasks when the first one is ready:
 * 翻译：假设你想使用任务集中的第一个非空结果（忽略任何遇到异常的任务），并在第一个任务准备好时取消所有其他任务：
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     int n = solvers.size();
 *     List<Future<Result>> futures
 *         = new ArrayList<Future<Result>>(n);
 *     Result result = null;
 *     try {
 *         for (Callable<Result> s : solvers)
 *             futures.add(ecs.submit(s));
 *         for (int i = 0; i < n; ++i) {
 *             try {
 *                 Result r = ecs.take().get();
 *                 if (r != null) {
 *                     result = r;
 *                     break;
 *                 }
 *             } catch (ExecutionException ignore) {}
 *         }
 *     }
 *     finally {
 *         for (Future<Result> f : futures)
 *             f.cancel(true);
 *     }
 *
 *     if (result != null)
 *         use(result);
 * }}</pre>
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final Executor executor;
    private final AbstractExecutorService aes; // aes 不能指定，只能由 executor 的类型来决定是否为 null。用在这里是为了使用其 newTaskFor 方法
    private final BlockingQueue<Future<V>> completionQueue;

    /**
     * FutureTask extension to enqueue upon completion
     * 扩展 FutureTask，完成后入队
     */
    private class QueueingFuture extends FutureTask<Void> {
        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);
            this.task = task;
        }
        // 重写 done() 方法，添加入队逻辑
        protected void done() { completionQueue.add(task); }
        private final Future<V> task;
    }

    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if (aes == null)
            // 如果 executor 不是 AbstractExecutorService 的子类，则默认使用 FutureTask
            return new FutureTask<V>(task);
        else
            // 委托 executor 中的 newTaskFor 方法
            return aes.newTaskFor(task);
    }

    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if (aes == null)
            // 如果 executor 不是 AbstractExecutorService 的子类，则默认使用 FutureTask
            return new FutureTask<V>(task, result);
        else
            // 委托 executor 中的 newTaskFor 方法
            return aes.newTaskFor(task, result);
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and a
     * {@link LinkedBlockingQueue} as a completion queue.
     * 翻译：使用 为执行基础任务提供的 executor 和一个作为完成队列的 LinkedBlockingQueue 来创建一个 ExecutorCompletionService。
     *
     * @param executor the executor to use
     * @throws NullPointerException if executor is {@code null} 如果 executor 为 null，则抛出 NullPointerException
     */
    public ExecutorCompletionService(Executor executor) {
        if (executor == null)
            throw new NullPointerException();
        this.executor = executor;
        // 如果 executor 不是 AbstractExecutorService 的子类则为 null，否则将 executor 强转为 AbstractExecutorService
        this.aes = (executor instanceof AbstractExecutorService) ?
            (AbstractExecutorService) executor : null;
        // 如果不指定 queue，则默认为 LinkedBlockingQueue 无界队列（界为 Integer.MAX_VALUE）
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and the supplied queue as its
     * completion queue.
     * 翻译：使用 为执行基础任务提供的 executor 和 作为完成队列提供的 queue 来创建一个 ExecutorCompletionService。
     *
     * @param executor the executor to use
     * @param completionQueue the queue to use as the completion queue
     *        normally one dedicated for use by this service. This
     *        queue is treated as unbounded -- failed attempted
     *        {@code Queue.add} operations for completed tasks cause
     *        them not to be retrievable.
     *                        用作完成队列的队列通常专供此服务使用。此队列被视为无界 -- 已完成任务尝试 Queue.add 操作失败会导致它们无法检索。
     * @throws NullPointerException if executor or completionQueue are {@code null} 如果 executor 或 completionQueue 为 null，则抛出 NullPointerException
     */
    public ExecutorCompletionService(Executor executor,
                                     BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null)
            throw new NullPointerException();
        this.executor = executor;
        // 如果 executor 不是 AbstractExecutorService 的子类则为 null，否则将 executor 强转为 AbstractExecutorService
        this.aes = (executor instanceof AbstractExecutorService) ?
            (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;
    }

    // 和 AbstractExecutorService 提交逻辑差不多，只是构造了不同的 RunnableFuture 实现
    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    // 和 AbstractExecutorService 提交逻辑差不多，只是构造了不同的 RunnableFuture 实现
    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task, result);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    public Future<V> take() throws InterruptedException {
        // 委托 completionQueue 中的 take 方法
        return completionQueue.take();
    }

    public Future<V> poll() {
        // 委托 completionQueue 中的 poll 方法
        return completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        // 委托 completionQueue 中的 poll 方法
        return completionQueue.poll(timeout, unit);
    }

}
