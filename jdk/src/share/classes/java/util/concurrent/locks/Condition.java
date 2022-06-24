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

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 * 翻译：Condition 将 Object 监视器方法（Object#wait()、Object#notify 和 Object#notifyAll）分解为不同的对象，以产生每个对象有多个等待集合的效果，通过将它们与任意 Lock 的实现结合使用。Lock 代替了 synchronized 方法和语句的使用，Condition 代替了 Object 监视器方法的使用。
 *
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 * 翻译：条件（也称为 条件队列 或 条件变量）为一个线程提供一种挂起执行（以"等待”）的方法，直到被另一个线程通知某个状态条件现在可能成立了。因为对该共享状态信息的访问发生在不同的线程中，所以它必须受到保护，因此某种形式的锁与条件是相关联的。等待条件 提供的关键属性是它 原子地 释放关联的锁并挂起当前线程，就像 Object.wait 方法一样。
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 * 翻译：Condition 实例本质上绑定到锁。要获取特定 Lock 实例的 Condition 实例，请使用其 Lock#newCondition 方法。
 *
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 * 翻译：例如，假设我们有一个支持 put 和 take 方法的有界缓冲区。如果在空缓冲区上尝试 take，则线程将阻塞，直到元素变为可用；如果在满缓冲区上尝试 put，则线程将阻塞，直到有空间可用。我们希望在单独的等待集合中继续等待 put 线程和 take 线程，以便我们可以使用 当缓冲区中的元素或空间变得可用时每次只通知单个线程 的优化。
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 * 翻译：（java.util.concurrent.ArrayBlockingQueue 类提供此功能，因此没有理由实现此示例用法类。）
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 * 翻译：Condition 实现可以提供与 Object 监视器方法不同的行为和语义，例如保证通知的顺序，或者在执行通知时不需要锁。如果实现提供了这种专门的语义，那么实现必须记录这些语义。
 *
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 * 翻译：注意，Condition 实例只是普通对象，它们本身可以用作 synchronized 语句中的目标，并且可以调用它们自己的监视器等待（Object#wait）和通知（Object#notify）方法。获取 Condition 实例的监视器锁，或使用其监视器方法，与 获取与该 Condition 关联的 Lock 或使用其等待（await）和发信号（signal）方法没有指定关系。建议不要以这种方式使用 Condition 实例以避免混淆，除非在它们自己的实现中。
 *
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 * 翻译：除非另有说明，否则为任何参数传递 null 值都将导致抛出 NullPointerException。
 *
 * <h3>Implementation Considerations</h3>
 * <h3>实现注意事项</h3>
 *
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 * 翻译：在等待 Condition 时，通常允许发生 “虚假唤醒”，作为对底层平台语义的 让步/妥协。这对大多数应用程序几乎没有实际影响，因为 Condition 应该始终在循环中等待，测试正在等待的状态谓词。实现可以自由地消除虚假唤醒的可能性，但建议程序员始终假设它们可能发生，因此总是在循环中等待。
 *
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 * 翻译：三种形式的条件等待（可中断、不可中断和定时）在某些平台上实现的难易程度和性能特征上可能有所不同。特别是，可能很难提供这些特性并维护特定语义，例如顺序保证。此外，在所有平台上实现中断实际挂起线程的能力并不总是可行的。
 *
 * <p>consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 * 翻译：因此，实现不需要为所有三种等待形式定义完全相同的保证或语义，也不需要支持线程实际挂起的中断。
 *
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 * 翻译：实现需要清楚地记录每个等待方法提供的语义和保证，并且当实现确实支持线程挂起的中断时，必须遵守此接口中定义的中断语义。
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 * 翻译：由于中断通常意味着取消，并且对中断的检查通常很少，因此实现更倾向于响应中断而不是正常的方法返回。即使可以证明 中断发生在另一个可能已解除线程阻塞的操作之后 也是如此。实现应记录此行为。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     * 翻译：使当前线程等待，直到它收到信号或被中断（Thread#interrupt）
     *
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 翻译：与此 Condition 关联的锁被自动释放，当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下四种情况之一：
     * 1) 其他某些线程为此 Condition 调用 signal 方法，并且当前线程恰好被选为要唤醒的线程；
     * 2) 其他某些线程为此 Condition 调用 signalAll 方法；
     * 3) 其他某些线程中断（Thread#interrupt）当前线程，并且支持线程挂起中断；
     * 4) 发生 “虚假唤醒”。
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 翻译：在所有情况下，在此方法可以返回之前，当前线程必须重新获取与此 condition 关联的锁。当线程返回时，保证持有这个锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * If the current thread is interrupted while waiting and interruption of thread suspension is supported,
     * 翻译：如果当前线程：
     * 1) 在进入此方法时设置了其中断状态；
     * 2) 在等待时被中断（Thread#interrupt），并且支持线程挂起中断；
     * 然后抛出 InterruptedException 并清除当前线程的中断状态。对于第一种情况，没有规定是否在释放锁之前进行中断测试。
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 翻译：调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现来确定是否是这种情况，如果不是如何响应。通常，会抛出异常（例如 IllegalMonitorStateException）并且实现必须记录该事实。
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 翻译：实现更倾向于响应中断而不是响应信号的正常方法返回。在这种情况下，实现必须确保信号被重定向到另一个等待线程（如果有的话）。
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     *         如果当前线程被中断（并且支持线程挂起中断），则抛出 InterruptedException
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.
     * 翻译：使当前线程等待，直到它收到信号。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 翻译：与此 Condition 关联的锁被自动释放，当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     * 1) 其他某些线程为此 Condition 调用 signal 方法，并且当前线程恰好被选为要唤醒的线程；
     * 2) 其他某些线程为此 Condition 调用 signalAll 方法；
     * 3) 发生 “虚假唤醒”。
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 翻译：在所有情况下，在此方法可以返回之前，当前线程必须重新获取与此 condition 关联的锁。当线程返回时，保证持有这个锁。
     *
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     * 翻译：如果当前线程进入此方法时设置了中断状态，或者在等待中被中断（Thread#interrupt），则将继续等待直到收到信号。当它最终从这个方法返回时，它的中断状态仍将被设置。
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 翻译：调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现来确定是否是这种情况，如果不是如何响应。通常，会抛出异常（例如 IllegalMonitorStateException）并且实现必须记录该事实。
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     * 翻译：使当前线程等待，直到它收到信号或被中断，或指定的等待时间已过。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 翻译：与此 Condition 关联的锁被自动释放，当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下五种情况之一：
     * 1) 其他某些线程为此 Condition 调用 signal 方法，并且当前线程恰好被选为要唤醒的线程；
     * 2) 其他某些线程为此 Condition 调用 signalAll 方法；
     * 3) 其他某些线程中断（Thread#interrupt）当前线程，并且支持线程挂起中断；
     * 4) 指定的等待时间已过；
     * 5) 发生 “虚假唤醒”。
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 翻译：在所有情况下，在此方法可以返回之前，当前线程必须重新获取与此 condition 关联的锁。当线程返回时，保证持有这个锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 翻译：如果当前线程：
     * 1) 在进入此方法时设置了其中断状态；
     * 2) 在等待时被中断（Thread#interrupt），并且支持线程挂起中断；
     * 然后抛出 InterruptedException 并清除当前线程的中断状态。对于第一种情况，没有规定是否在释放锁之前进行中断测试。
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     * 翻译：该方法返回 给定返回时提供的 nanosTimeout 值的 估计的剩余等待纳秒数，如果超时则返回小于或等于 0 的值。在等待返回但等待条件仍然不成立的情况下，此值可用于确定是否重新等待以及重新等待多长时间。此方法的典型用法如以下形式：
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     * 翻译：设计说明：此方法需要纳秒参数，以避免报告剩余时间时出现截断错误。这种精度损失将使程序员难以确保总等待时间不会系统地短于重新等待发生时指定的时间。
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 翻译：调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现来确定是否是这种情况，如果不是如何响应。通常，会抛出异常（例如 IllegalMonitorStateException）并且实现必须记录该事实。
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 翻译：与响应信号的正常方法返回或指示指定的等待时间已过相比，实现更倾向于响应中断。在任何一种情况下，实现必须确保信号被重定向到另一个等待线程（如果有的话）。
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     *                     最长等待时间，单位纳秒
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     *         返回 nanosTimeout 值 减去 等待此方法返回所花费的时间 的估计值。正值可用作 后续调用此方法以完成等待所需时间 的参数。小于或等于 0 的值表示没有剩余时间。
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     *         如果当前线程被中断（并且支持线程挂起中断），则抛出 InterruptedException
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *  翻译：使当前线程等待，直到它收到信号或被中断，或指定的等待时间已过。此方法在行为上等效于：awaitNanos(unit.toNanos(time)) > 0
     *
     * @param time the maximum time to wait
     *             最长等待时间
     * @param unit the time unit of the {@code time} argument
     *             time 参数的时间单位
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     *         如果 从方法返回之前 检测到等待时间已过，则返回 false；否则返回 true
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     *         如果当前线程被中断（并且支持线程挂起中断），则抛出 InterruptedException
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     * 翻译：使当前线程等待，直到它收到信号或被中断，或指定的 deadline 已过。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 翻译：与此 Condition 关联的锁被自动释放，当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下五种情况之一：
     * 1) 其他某些线程为此 Condition 调用 signal 方法，并且当前线程恰好被选为要唤醒的线程；
     * 2) 其他某些线程为此 Condition 调用 signalAll 方法；
     * 3) 其他某些线程中断（Thread#interrupt）当前线程，并且支持线程挂起中断；
     * 4) 指定的 deadline 已过；
     * 5) 发生 “虚假唤醒”。
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 翻译：在所有情况下，在此方法可以返回之前，当前线程必须重新获取与此 condition 关联的锁。当线程返回时，保证持有这个锁。
     *
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 翻译：如果当前线程：
     * 1) 在进入此方法时设置了其中断状态；
     * 2) 在等待时被中断（Thread#interrupt），并且支持线程挂起中断；
     * 然后抛出 InterruptedException 并清除当前线程的中断状态。对于第一种情况，没有规定是否在释放锁之前进行中断测试。
     *
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     * 翻译：返回值表示是否已过 deadline，可像如下使用：
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 翻译：调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现来确定是否是这种情况，如果不是如何响应。通常，会抛出异常（例如 IllegalMonitorStateException）并且实现必须记录该事实。
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *        响应信号的正常方法返回，或者更倾向于指示指定截止日期的过去
     * 翻译：与响应信号的正常方法返回或指示指定的 deadline 已过相比，实现更倾向于响应中断。在任何一种情况下，实现必须确保信号被重定向到另一个等待线程（如果有的话）。
     *
     * @param deadline the absolute time to wait until
     *                 要等到的绝对时间
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     *         如果返回时已过 deadline，则返回 false；否则返回 true
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     *         如果当前线程被中断（并且支持线程挂起中断），则抛出 InterruptedException
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     * 翻译：唤醒一个等待线程。
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     * 翻译：如果有任何线程正在此条件下等待，则选择一个去唤醒。此后，该线程必须 在从 await 返回之前 重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     * 翻译：当调用此方法时，实现可能（并且通常确实）要求当前线程持有与此 Condition 关联的锁。实现必须记录此前提条件以及在未持有锁时采取的任何操作。通常，将抛出诸如 IllegalMonitorStateException 之类的异常。
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     * 翻译：唤醒所有等待线程。
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     * 翻译：如果有任何线程正在此条件下等待，那么它们都会被唤醒。每个线程必须重新获取锁才能从 await 返回。
     *
     * <p><b>Implementation Considerations</b>
     * <b>实现注意事项</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     * 翻译：当调用此方法时，实现可能（并且通常确实）要求当前线程持有与此 Condition 关联的锁。实现必须记录此前提条件以及在未持有锁时采取的任何操作。通常，将抛出诸如 IllegalMonitorStateException 之类的异常。
     */
    void signalAll();
}
