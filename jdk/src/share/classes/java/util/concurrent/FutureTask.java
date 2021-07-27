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
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 * 翻译：可取消的异步计算。此类提供了 Future 的基本实现，具有启动和取消计算、查询计算是否完成以及检索计算结果的方法。结果只能在计算完成后检索；如果计算尚未完成，get 方法将阻塞。一旦计算完成，就不能重新开始或取消计算（除非使用 runAndReset 调用计算）。
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 * 翻译：FutureTask 可用于包装 Callable 或 Runnable 对象。因为 FutureTask 实现了 Runnable，所以 FutureTask 可以提交给 Executor 执行。
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 * 翻译：除了作为一个独立的类外，此类还提供 protected 的功能，这在创建自定义任务类时可能很有用。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 *           V 是此 FutureTask 的 get 方法返回的结果类型
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     * 翻译：修订说明：这与依赖 AQS 的此类以前的版本不同，主要是为了避免用户 在取消竞争期间保留中断状态而 感到奇怪。当前设计中的同步控制依赖于通过 CAS 更新的 “state” 字段来跟踪完成，以及一个简单的 Treiber stack 来保存等待线程。
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     * 翻译：样式说明：像往常一样，我们绕过使用 AtomicXFieldUpdaters 的开销，而是直接使用 Unsafe 内部函数。
     */

    /**
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.  During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     * 翻译：此任务的运行状态初始是 NEW。运行状态仅在 set、setException 和 cancel 方法中转换为终止状态。在完成期间，状态可能采用 COMPLETING（在设置 outcome 时）或 INTERRUPTING（仅在中断运行程序以满足 cancel(true) 时）的瞬态值。从这些中间状态到最终状态的转换使用 更廉价 的 有序/延迟 写入，因为值是唯一的且无法进一步修改。
     *
     * Possible state transitions:
     * 翻译：可能的状态转换：
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0; // 初始值：任务创建和执行中
    private static final int COMPLETING   = 1; // 瞬态值：正常执行完成 或 抛出异常，但是 result 或 ex 还没有赋值给 outcome。在 set() 或 setException() 调用中设置
    private static final int NORMAL       = 2; // 终态值：正常执行结束。在 set() 调用中设置
    private static final int EXCEPTIONAL  = 3; // 终态值：抛出异常结束。在 setException() 调用中设置
    private static final int CANCELLED    = 4; // 终态值：取消成功。在 cancel(false) 调用中设置
    private static final int INTERRUPTING = 5; // 瞬态值：打断中。在 cancel(true) 调用中设置
    private static final int INTERRUPTED  = 6; // 终态值：打断成功。在 cancel(true) 调用中设置

    /** The underlying callable; nulled out after running */
    /** 内部的 callable；运行后清空 */
    private Callable<V> callable;
    /** The result to return or exception to throw from get() */
    /** 从 get() 返回的结果或抛出的异常 */
    private Object outcome; // non-volatile, protected by state reads/writes 非 volatile 的，受状态 读/写 保护
    /** The thread running the callable; CASed during run() */
    /** 运行 callable 的线程；在 run() 方法中通过 CAS 设置 */
    // 运行当前任务的线程
    private volatile Thread runner;
    /** Treiber stack of waiting threads */
    /** 等待线程的 Treiber stack */
    // 调用 get 方法时等待的线程，Treiber stack 头节点
    private volatile WaitNode waiters;

    /**
     * Returns result or throws exception for completed task.
     * 翻译：为完成的任务返回结果或抛出异常。
     *
     * 其实就是根据不同的状态返回 outcome 的值
     *
     * @param s completed state value 完成的状态值
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        // 正常执行结束
        if (s == NORMAL)
            return (V)x;
        // 取消成功（CANCELLED） / 打断中（INTERRUPTING） / 打断成功（INTERRUPTED）
        if (s >= CANCELLED)
            throw new CancellationException();
        // 抛出异常结束
        throw new ExecutionException((Throwable)x);
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     * 翻译：创建一个 FutureTask，它将在运行时执行给定的 Callable。
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null 如果 callable 为 null，则抛出 NullPointerException
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable 确保 callable 的可见性
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     * 翻译：创建一个 FutureTask，它将在运行时执行给定的 Runnable，并安排 get 在成功完成后返回给定的结果。
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     *               成功完成后返回的结果。如果你不需要特定的结果，请考虑使用以下形式的构造：
     *               Future<?> f = new FutureTask<Void>(runnable, null)
     * @throws NullPointerException if the runnable is null 如果 runnable 为 null，则抛出 NullPointerException
     */
    public FutureTask(Runnable runnable, V result) {
        // Executors.callable 方法把 runnable 适配成 RunnableAdapter，RunnableAdapter 实现了 Callable，是一个运行给定任务并返回给定结果的 Callable
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable 确保 callable 的可见性
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        // [state != NEW] 或 [[state == NEW] 但 [state 由 NEW 变为 INTERRUPTING 或 CANCELLED] 失败]，直接返回 false
        // 此处可能出现的状态转换为：NEW -> CANCELLED（终态） 或 NEW -> INTERRUPTING（还未到终态，瞬态值）
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {    // in case call to interrupt throws exception （try 用来）防止调用中断抛出异常
            // 如果执行此任务的线程应该被中断，那就调用 interrupt 进行中断
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    // 最终状态设置为 INTERRUPTED
                    // 状态转换为：NEW -> INTERRUPTING -> INTERRUPTED
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 删除并唤醒所有在 Treiber stack 中排队的等待线程去拿结果
            finishCompletion();
        }
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 当前状态是 NEW / COMPLETING 时，等待计算完成
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        // 返回结果
        return report(s);
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        // 当前状态是 NEW / COMPLETING 且 超时，抛出 TimeoutException
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        // 返回结果
        return report(s);
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     * 翻译：当此任务转换到状态 isDone 时调用的 protected 方法（无论是正常结束还是通过取消）。默认实现什么都不做。子类可以重写此方法以调用完成回调或做记录。注意，你可以在此方法的实现里查询状态，以确定此任务是否已被取消。
     */
    protected void done() { }

    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     * 翻译：将此 future 的结果设置为给定值，除非此 future 已被设置或已被取消。
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     * 翻译：此方法在计算成功完成后由 run 方法内部调用。
     *
     * @param v the value 计算结果
     */
    protected void set(V v) {
        // state 由 NEW 变为 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 将 result 赋值给 outcome
            outcome = v;
            // 最终状态设置为 NORMAL
            // 状态转换为：NEW -> COMPLETING -> NORMAL
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            // 删除并唤醒所有在 Treiber stack 中排队的等待线程去拿结果
            finishCompletion();
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     * 翻译：导致此 future report 一个以给定 throwable 作为其原因的 ExecutionException，除非此 future 已被设置或已被取消。
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     * 翻译：此方法在计算失败时由 run 方法内部调用。
     *
     * @param t the cause of failure 失败原因
     */
    protected void setException(Throwable t) {
        // state 由 NEW 变为 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 将 ex 赋值给 outcome
            outcome = t;
            // 最终状态设置为 EXCEPTIONAL
            // 状态转换为：NEW -> COMPLETING -> EXCEPTIONAL
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            // 删除并唤醒所有在 Treiber stack 中排队的等待线程去拿结果
            finishCompletion();
        }
    }

    public void run() {
        // [state != NEW] 或 [[state == NEW] 但 [runner 由 null 赋值为 currentThread] 失败]，直接返回
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                // 是否执行成功
                boolean ran;
                try {
                    // 调用执行，获取结果
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    // 执行过程中抛出异常，将 ex 赋值给 outcome
                    setException(ex);
                }
                if (ran)
                    // 正常执行结束，将 result 赋值给 outcome
                    set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // 翻译：runner 必须非空，直到状态稳定，以防止并发调用 run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // 翻译：runner 置为 null 后必须重新读取状态，以防止漏掉中断
            int s = state;
            // 有可能此时线程正在打断中（比如调用了 cancel(true) 方法），那就等待状态到达终态
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     * 翻译：执行计算而不设置其结果，然后将此 future 重置为初始状态，如果计算遇到异常或被取消，则不能这样做。这是专为本质上执行多次的任务而设计的。
     *
     * @return {@code true} if successfully run and reset 如果成功运行并成功重置状态，则返回 true
     */
    protected boolean runAndReset() {
        // [state != NEW] 或 [[state == NEW] 但 [runner 由 null 赋值为 currentThread] 失败]，直接返回
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result 不设置结果
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // 翻译：runner 必须非空，直到状态稳定，以防止并发调用 run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // 翻译：runner 置为 null 后必须重新读取状态，以防止漏掉中断
            s = state;
            // 有可能此时线程正在打断中（比如调用了 cancel(true) 方法），那就等待状态到达终态
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        // 执行成功，状态置为 NEW
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     * 翻译：确保 有可能来自 cancel(true) 的 任何中断 仅在 run 或 runAndReset 时传递给任务。
     *
     * run / runAndReset 会调用此方法
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        // 翻译：interrupter 有可能在有机会打断我们之前 就停止了。让我们耐心的自旋等待。
        // 等待，直到 state 由 INTERRUPTING 变为 INTERRUPTED，到达最终状态
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        // 翻译：我们想清除我们可能从 cancel(true) 收到的任何中断。但是，使用中断作为任务与其调用者通信的独立机制是允许的，并且没有办法仅清除取消中断。
        //
        // Thread.interrupted();
    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     * 翻译：记录 Treiber stack 中等待线程的简单链表节点。有关更详细的说明，请参阅其他类，例如 Phaser 和 SynchronousQueue。
     *
     * 单向链表
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     * 翻译：删除并通知所有等待线程，调用 done() 方法，并将 callable 设为 null。
     *
     * cancel / set / setException 会调用此方法，也就是状态到 终态 了才会调用此方法
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            // waiters 由 q 赋值为 null
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                // 状态到达终态时，遍历链表，唤醒所有阻塞的线程去拿结果
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        // 唤醒阻塞的线程去拿结果
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    // 遍历到链表尾部结束
                    if (next == null)
                        break;
                    // 断开头节点，help gc
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     * 翻译：在中断或超时时等待完成或中止。
     *
     * @param timed true if use timed waits 如果是定时等待（即等待特定时间），则为 true；false 表示一直等待
     * @param nanos time to wait, if timed 定时等待时的等待时间
     * @return state upon completion 完成时的状态
     *
     * get() / get(long timeout, TimeUnit unit) 会调用此方法
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        // 计算等待终止时间
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        // 当前线程对应的 WaitNode 是否放到了 Treiber stack（单向链表）中
        boolean queued = false;
        for (;;) {
            // 如果线程已经被中断，则删除该节点并抛出 InterruptedException 异常
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }

            // 获取当前状态
            int s = state;
            /* 以下都是 if...else 分支，只有一个分支会进，然后进入下一次循环 */
            // 当前任务已经到达某个终态（NORMAL / EXCEPTIONAL / CANCELLED / INTERRUPTED），返回当前终态
            if (s > COMPLETING) {
                if (q != null)
                    // 当前线程对应的 WaitNode 中的 thread 置为 null
                    q.thread = null;
                return s;
            }
            // 当前任务已经 正常执行完成 或 抛出异常，调用 yield 等待设置 outcome
            else if (s == COMPLETING) // cannot time out yet 还不能超时
                Thread.yield();
            else if (q == null)
                /* 第一次非常大概率走到该分支 */
                q = new WaitNode();
            else if (!queued)
                /* 第二次非常大概率走到该分支 */
                /* Treiber stack 算法入栈逻辑 */
                // waiters 由 旧的 waiters（old head） 赋值为 q（new head），同时 q.next 指向 旧的 waiters（旧 head）
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);
            /* 第三次大概率走到以下两个分支中的某一个，去阻塞等待 */
            // 如果调用的是 get(long timeout, TimeUnit unit) 方法等待计算结果
            else if (timed) {
                // 计算要等待的时间
                nanos = deadline - System.nanoTime();
                // 判断超时，超时则删除对应节点并返回当前状态
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                // 如果未超时，阻塞等待特定时间。线程进入 TIMED_WAITING 状态
                // 在 finishCompletion() 方法（状态到达终态）中会进行唤醒
                LockSupport.parkNanos(this, nanos);
            }
            // 如果调用的是 get() 方法等待计算结果
            else
                // 阻塞等待，直到被其他线程唤醒。线程进入 WAITING 状态
                // 在 finishCompletion() 方法（状态到达终态）中会进行唤醒
                LockSupport.park(this);
        }
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     * 翻译：尝试断开超时或中断的等待节点的连接以避免积累垃圾。内部节点在没有 CAS 的情况下只是未拼接，因为即使它们被 releasers 无论如何遍历都是无害的。为了避免从已经删除的节点中解开的影响，如果出现明显的竞争，则对列表进行 回溯（retraversed 什么意思？）。当有很多节点时，这很慢，但我们不希望列表足够长以超过更高开销的方案。
     *
     * 只有 awaitDone(boolean timed, long nanos) 方法中会调用
     */
    private void removeWaiter(WaitNode node) {
        // 该方法 node 不为 null 时才会调用
        if (node != null) {
            // 断开该 node 前先将 WaitNode 中的 thread 置为 null，这个操作和下面的 if...else 逻辑有关
            node.thread = null;
            // continue label 写法：两个 for 循环，continue 时要跳转出外循环
            // 每次从链表头部节点开始遍历
            retry:
            for (;;) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    // q 的下一个节点
                    s = q.next;
                    /* 以下都是 if...else 分支，只有一个分支会进，然后进入下一次循环 */
                    if (q.thread != null)
                        pred = q;
                    // q.thread == null，则从队列中删除 q（q 的前一个节点 pred 指向 q 的下一个节点 s）
                    else if (pred != null) {
                        pred.next = s;
                        // 如果 q 的前一个节点 pred 中的 thread 为 null，说明此刻有竞争（可能 state 到终态；可能线程被打断；可能是阻塞等待超时），跳出外循环又从头节点开始遍历
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    /* Treiber stack 算法出栈逻辑 */
                    // q.thread == null && pred == null，说明此时 q 为头节点。将 waiters 由 q 赋值为它的下一个节点 s（从队列中删除 q）
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
