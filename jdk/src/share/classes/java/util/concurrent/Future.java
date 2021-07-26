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
 * A {@code Future} represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.  The result can only be retrieved using method
 * {@code get} when the computation has completed, blocking if
 * necessary until it is ready.  Cancellation is performed by the
 * {@code cancel} method.  Additional methods are provided to
 * determine if the task completed normally or was cancelled. Once a
 * computation has completed, the computation cannot be cancelled.
 * If you would like to use a {@code Future} for the sake
 * of cancellability but not provide a usable result, you can
 * declare types of the form {@code Future<?>} and
 * return {@code null} as a result of the underlying task.
 * 翻译：Future 表示异步计算的结果。提供检查计算是否完成、等待计算完成以及检索计算结果的方法。结果只能在计算完成后使用 get 方法检索，必要时会阻塞直到计算结果 ready。取消由 cancel 方法执行。还提供了其他方法来确定任务是正常完成还是被取消。一旦计算完成，就不能取消计算。如果为了可取消性的目的想使用 Future 但不提供可用的结果，你可以声明形式为 Future<?> 的类型并返回 null 作为底层任务的结果。
 *
 * <p>
 * <b>Sample Usage</b> (Note that the following classes are all
 * made-up.)
 * 翻译：注意，以下类都是虚构的
 * <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * The {@link FutureTask} class is an implementation of {@code Future} that
 * implements {@code Runnable}, and so may be executed by an {@code Executor}.
 * For example, the above construction with {@code submit} could be replaced by:
 * FutureTask 类是实现 Runnable 的 Future 的实现，因此可以由 Executor 执行。例如，上面 submit 可以替换为：
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * <p>Memory consistency effects: Actions taken by the asynchronous computation
 * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
 * actions following the corresponding {@code Future.get()} in another thread.
 * 翻译：内存一致性影响：异步计算采取的操作 happen-before 在另一个线程中相应的 Future.get() 之后的操作。
 *
 * @see FutureTask
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method
 *           V 是此 Future 的 get 方法返回的结果类型
 */
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     * 翻译：尝试取消此任务的执行。如果任务已完成、已被取消或由于其他原因无法取消，则此尝试将失败。如果成功，并且当调用 cancel 时此任务尚未启动，则此任务不会运行。如果任务已经启动，那么 mayInterruptIfRunning 参数决定是否应该中断执行此任务的线程以尝试停止该任务。
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     * 翻译：此方法返回后，后续调用 isDone 方法将始终返回 true。如果此方法返回 true，后续调用 isCancelled 方法将始终返回 true。
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     *                                          如果执行此任务的线程应该被中断，则为 true；否则，允许正在进行的任务完成
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     * 如果任务无法取消，则返回 false，通常是因为任务已正常完成；否则返回 true
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     * 翻译：如果此任务在正常完成之前被取消，则返回 true。
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     * 翻译：如果此任务完成，则返回 true。
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     * 翻译：完成可能是由于正常终止、异常或取消操作 -- 在所有这些情况下，此方法将返回 true。
     *
     * @return {@code true} if this task completed
     */
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 翻译：如有必要，等待计算完成，然后检索其结果。
     *
     * @return the computed result 计算结果
     * @throws CancellationException if the computation was cancelled 如果计算被取消，则抛出 CancellationException
     * @throws ExecutionException if the computation threw an
     * exception 如果计算抛出异常，则抛出 ExecutionException
     * @throws InterruptedException if the current thread was interrupted
     * while waiting 如果当前线程在等待时被中断，则抛出 InterruptedException
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     * 翻译：如有必要，最多等待给定的计算完成时间，然后检索其结果（如果可用）。
     *
     * @param timeout the maximum time to wait 最长等待时间
     * @param unit the time unit of the timeout argument （timeout）参数的时间单位
     * @return the computed result 计算结果
     * @throws CancellationException if the computation was cancelled 如果计算被取消，则抛出 CancellationException
     * @throws ExecutionException if the computation threw an
     * exception 如果计算抛出异常，则抛出 ExecutionException
     * @throws InterruptedException if the current thread was interrupted
     * while waiting 如果当前线程在等待时被中断，则抛出 InterruptedException
     * @throws TimeoutException if the wait timed out 如果等待超时，则抛出 TimeoutException
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
