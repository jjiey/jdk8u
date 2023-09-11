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
 * A service that decouples the production of new asynchronous tasks
 * from the consumption of the results of completed tasks.  Producers
 * {@code submit} tasks for execution. Consumers {@code take}
 * completed tasks and process their results in the order they
 * complete.  A {@code CompletionService} can for example be used to
 * manage asynchronous I/O, in which tasks that perform reads are
 * submitted in one part of a program or system, and then acted upon
 * in a different part of the program when the reads complete,
 * possibly in a different order than they were requested.
 * 翻译：一个将 消费已完成任务的结果，产生新的异步任务 解耦的服务。生产者 submit 执行去任务。消费者 take 完成的任务并按照它们完成的顺序处理它们的结果。例如，CompletionService 可用于管理异步 IO，其中执行读取的任务在程序或系统的一部分中提交，然后在读取完成时在程序的不同部分执行操作，可能与请求的顺序不同。
 *
 * <p>Typically, a {@code CompletionService} relies on a separate
 * {@link Executor} to actually execute the tasks, in which case the
 * {@code CompletionService} only manages an internal completion
 * queue. The {@link ExecutorCompletionService} class provides an
 * implementation of this approach.
 * 翻译：通常，CompletionService 依赖一个单独的 Executor 来实际执行任务，在这种情况下，CompletionService 仅管理内部完成队列。ExecutorCompletionService 类提供了这种方法的实现。
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a task to a {@code CompletionService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions taken by that task, which in turn <i>happen-before</i>
 * actions following a successful return from the corresponding {@code take()}.
 * 翻译：内存一致性影响：将任务提交给 CompletionService 之前线程中的操作 先行发生于 该任务采取的操作 先行发生于 从相应的 take() 成功返回后的操作。
 */
public interface CompletionService<V> {
    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task.  Upon completion,
     * this task may be taken or polled.
     * 翻译：提交一个有返回值的任务以供执行，并返回一个表示任务未决结果的 Future。完成后，可以 take() 或 poll() 此任务。
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task 返回代表待完成任务的 Future
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     *         如果任务不能被调度执行，则抛出 RejectedExecutionException
     * @throws NullPointerException if the task is null 如果任务为 null，则抛出 NullPointerException
     */
    Future<V> submit(Callable<V> task);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task.  Upon completion, this task may be
     * taken or polled.
     * 翻译：提交一个 Runnable 任务以供执行，并返回一个代表该任务的 Future。完成后，可以 take() 或 poll() 此任务。
     *
     * @param task the task to submit
     * @param result the result to return upon successful completion 成功完成后返回的结果
     * @return a Future representing pending completion of the task,
     *         and whose {@code get()} method will return the given
     *         result value upon completion
     *         返回代表待完成任务的 Future，其 get() 方法将在完成后返回给定的结果值
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     *         如果任务不能被调度执行，则抛出 RejectedExecutionException
     * @throws NullPointerException if the task is null 如果任务为 null，则抛出 NullPointerException
     */
    Future<V> submit(Runnable task, V result);

    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if none are yet present.
     * 翻译：检索并删除代表下一个已完成任务的 Future，如果还不存在则等待。
     *
     * @return the Future representing the next completed task 代表下一个已完成任务的 Future
     * @throws InterruptedException if interrupted while waiting 如果在等待时中断，则抛出 InterruptedException
     */
    Future<V> take() throws InterruptedException;

    /**
     * Retrieves and removes the Future representing the next
     * completed task, or {@code null} if none are present.
     * 翻译：检索并删除代表下一个已完成任务的 Future，如果不存在则为 null。
     *
     * @return the Future representing the next completed task, or
     *         {@code null} if none are present
     *         代表下一个已完成任务的 Future，如果不存在则为 null。
     */
    Future<V> poll();

    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if necessary up to the specified wait
     * time if none are yet present.
     * 翻译：检索并删除代表下一个已完成任务的 Future，如果还不存在，则在必要时等待指定的等待时间。
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     *                放弃前最长等待时间，以 unit 为单位
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     *             TimeUnit 确定如何解释 timeout 参数（timeout 参数的时间单位）
     * @return the Future representing the next completed task or
     *         {@code null} if the specified waiting time elapses
     *         before one is present
     *         返回代表下一个完成任务的 Future 或 null（如果在 Future 存在之前已经过去了指定的等待时间）
     * @throws InterruptedException if interrupted while waiting 如果在等待时中断，则抛出 InterruptedException
     */
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
