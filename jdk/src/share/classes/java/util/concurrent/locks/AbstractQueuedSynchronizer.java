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
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * translation 约定如下：
 * sync queue: 同步队列
 * condition queue: 条件队列
 * both sync queue and condition queue are wait queue: 同步队列 和 条件队列 都是 等待队列
 * exclusive: 独占
 * shared: 共享
 * acquire: 获取
 * release: 释放
 * park: 挂起
 * unpark / wake up: 唤醒
 * enqueue: 入队
 * dequeue: 出队
 * predecessor: 前驱
 * successor: 后继
 *
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 * 翻译：提供一个框架，用于实现依赖先进先出 (FIFO) 等待队列的阻塞锁和相关同步器（信号量、事件等）。此类旨在成为 大多数依赖单个原子 int 值来表示状态的同步器 的有用基础。子类必须定义更改此状态值的 protected 方法，并定义该状态在获取或释放此对象的含义。鉴于这些，此类中的其他方法执行所有排队和阻塞机制。子类可以维护其他状态字段，但只有使用 #getState、#setState 和 #compareAndSetState 方法操作的原子更新的 int 值才会在同步方面进行跟踪。
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 * 翻译：子类应定义为非 public 内部帮助类，用于实现其封闭类的同步属性。AbstractQueuedSynchronizer 类没有实现任何同步接口。相反，它定义了诸如 #acquireInterruptibly 之类的方法，这些方法可以由具体锁和相关同步器酌情调用以实现它们的公共方法。
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 * 翻译：此类支持默认的 <em>独占<em> 模式和 <em>共享<em> 模式之一或两者。当以独占模式获取时，其他线程尝试获取不会成功。多个线程获取的共享模式可能（但不一定）成功。此类不 “理解” 这些差异，除了在机械(mechanical)意义上，即当共享模式获取成功时，下一个等待线程（如果存在）也必须确定它是否也可以获取。在不同模式下等待的线程共享同一个 FIFO 队列。通常，实现子类仅支持这些模式中的一种，但两种模式都可以发挥作用，例如 ReadWriteLock 类。仅支持独占或共享模式的子类不需要定义支持未使用模式的方法。
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 * 翻译：该类定义了一个嵌套的 ConditionObject 类，它可以被支持独占模式的子类用作 Condition 实现，其中 #isHeldExclusively 方法报告针对当前线程是否独占持有，使用当前 getState 方法返回值调用 release 方法完全释放此对象通过 #acquire 给定此保存的状态值，最终将此对象恢复到其之前获取的状态。AbstractQueuedSynchronizer 没有方法会创建这样的 condition，因此如果无法满足此约束，不要使用它。ConditionObject 的行为当然取决于其同步器实现的语义。
 * with respect to 关于；至于
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 * 翻译：此类为内部队列提供检查、检测和监视的方法，也为 condition 对象提供类似方法。这些可以根据需要导出到类中，使用 AbstractQueuedSynchronizer 作为它们的同步机制。
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 * 翻译：此类的序列化仅存储底层原子整数维护状态，因此反序列化的对象具有空线程队列。需要序列化的典型子类将定义 readObject 方法，该方法在反序列化时将其恢复到已知的初始状态。
 *
 * <h3>Usage</h3>
 * <h3>用法</h3>
 *
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 * 翻译：要将此类用作同步器的基础，可根据适用情况，通过使用 #getState、#setState 和/或 #compareAndSetState 方法检查 和/或 修改同步状态来重新定义以下方法：
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 * 翻译：默认情况下，这些方法中的每一个都会抛出 UnsupportedOperationException。这些方法的实现必须是内部线程安全的，并且通常应该是简短的不阻塞的。定义这些方法是 <em>唯一<em> 支持的使用此类的方法。所有其他方法都声明为 final，因为它们不能独立变化。
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 * 翻译：你可能还会发现 AbstractOwnableSynchronizer 的继承方法对于跟踪拥有独占同步器的线程很有用。鼓励你使用它们 -- 这使监视和诊断工具能够帮助用户确定哪些线程持有锁。
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 * 翻译：即使此类基于内部 FIFO 队列，它也不会自动执行 FIFO 采集策略。独占同步的核心形式为：
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        如果尚未排队，则将线程入队
 *        <em>possibly block current thread</em>;
 *        可能阻塞当前线程
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 *        取消阻塞第一个排队的线程
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 * 翻译：共享模式类似，但可能涉及级联信号。
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 * 翻译：因为在入队之前调用了对获取的检查，新的获取线程可能 <em>插入（barge）<em> 在其他被阻塞和排队的线程之前。但是，如果需要，你可以定义 tryAcquire 和/或 tryAcquireShared 方法以通过内部调用一种或多种检查方法来禁用插入，从而提供 <em>公平的<em> FIFO 获取顺序。特别是，如果 hasQueuedPredecessors（一种专门设计用于公平同步器使用的方法）方法返回 true，大多数公平同步器可以定义 tryAcquire 方法以返回 false。其他变化也是可能的。
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 * 翻译：默认插入（也称为 <em>greedy<em>、<em>renouncement<em> 和 <em>convoy-avoidance<em>）策略的吞吐量和可扩展性通常最高。虽然这不能保证公平或无饥饿，但允许较早的排队线程在较晚的排队线程之前重新竞争，并且每次重新竞争都有一个公平的机会成功对抗（against）传入的线程。此外，虽然获取在通常意义上不会 “自旋”，但它们可能会在阻塞之前执行多次调用 tryAcquire 方法并穿插其他计算。当仅短暂保持独占同步时，这提供了自旋的大部分好处，而在不保持时则没有大部分责任（liabilities）。如果需要，你可以通过在调用 acquire 方法之前使用 “快速路径” 检查来增强这一点，可能预先检查 hasContended 和/或 hasQueuedThreads 方法仅在同步器可能不竞争时才这样做。
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 * 翻译：此类 通过将其使用范围专门用于可以依赖 int 状态、获取和释放参数以及内部 FIFO 等待队列的同步器 为部分同步提供了高效且可扩展的基础。如果这还不够，你可以使用 java.util.concurrent.atomic 类、你自己自定义的 java.util.Queue 类和 LockSupport 阻塞支持从较低级别构建同步器。
 *
 * <h3>Usage Examples</h3>
 * <h3>使用示例</h3>
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 * 翻译：这是一个不可重入互斥锁类，它使用 0 表示 unlocked 状态，使用 1 表示 locked 状态。虽然不可重入锁并不严格要求记录当前所有者线程，但这个类无论如何都会这样做，以便更容易监视使用情况。它还支持 conditions 并公开一种检测方法：
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   // 内部帮助类
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     // 报告是否处于 locked 状态
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     // 如果 state 为 0，则获取锁
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused 否则未使用
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     // 通过将 state 设置为 0 来释放锁
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused 否则未使用
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     // 提供一个 Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     // 正确反序列化
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state 重置为 unlocked 状态
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   // sync 对象完成了所有的困难工作。我们直接使用它即可。
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 * 翻译：这是一个类似于 java.util.concurrent.CountDownLatch 的闩锁类，只是它只需要一个 signal 即可触发。由于闩锁是非独占的，因此它使用 shared acquire 和 release 方法。
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     * 创建一个新的 AbstractQueuedSynchronizer 实例，初始同步状态为零。
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     * 翻译：等待队列节点类。
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     * 翻译：等待队列是 “CLH”（Craig、Landin 和 Hagersten）锁队列的变体。CLH 锁通常用于自旋锁。我们将它们作为阻塞同步器用，但使用相同的基本策略，即在其节点的前驱中保存有关线程的一些控制信息。每个节点中的 “status” 字段跟踪线程是否应该阻塞。节点在其前驱 release 时会收到信号。队列中的每个节点都充当一个特定的通知式监视器，持有一个等待线程。状态字段不控制线程是否被授予锁等。如果线程是队列中的第一个，则可以尝试获取。但成为第一个并不能保证成功；这只给予它竞争的权利。所以当前已释放的竞争者线程可能也需要重新等待。
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * 翻译：要入 CLH 锁队列，你可以原子地将其拼接为新的 tail。要出队，你只需设置 head 字段。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     * 翻译：插入 CLH 队列只需要对 “tail” 进行一次原子操作，因此从未排队到排队有一个简单的原子分界点。类似地，出队只涉及更新 “head”。然而，节点需要做更多的工作来确定它们的后继是谁，部分是为了处理由于超时和中断可能导致的取消。
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     * “prev” 链接（未在原始 CLH 锁中使用）主要用于处理取消。如果一个节点被取消，它的后继（通常）会重新链接到一个未取消的前驱。有关自旋锁情况下类似机制的解释，请参阅 Scott 和 Scherer 的论文：http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     * 翻译：我们还使用 “next” 链接来实现阻塞机制。每个节点的线程 id 保存在它自己的节点中，因此前驱通过遍历下一个链接来确定它是哪个线程 来通知下一个节点唤醒。确定后继节点必须避免与新排队节点竞争，以设置其前驱节点的 “next” 字段。当节点的后继节点似乎为空时，通过从原子更新的 “tail” 向后检查，在必要时解决此问题。（或者，换句话说，next 指针是一种优化，我们通常不需要向后扫描。）
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     * 翻译：取消为基础算法引入了一些保守性。由于我们必须轮询其它节点的取消，因此我们可能无法注意到被取消的节点是在我们前面还是后面。这是通过 在取消时总是唤醒后继 来处理的，允许他们稳定在新的前驱上，除非我们能确定一个未取消的前驱将承担这个责任。
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     * 翻译：CLH 队列需要一个虚拟头节点来开始。但是我们不会在构建时创建它们，因为如果不存在竞争，那将是做无用功。相反，在第一次竞争时构造节点并设置头尾指针。
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     * 翻译：在 Conditions 中等待的线程使用相同的节点，但使用额外的链接。Conditions 只需要链接简单（非并发）链接队列中的节点，因为它们仅在独占持有时才会被访问。await 时，一个节点被插入到 condition queue 中。signal 时，节点被转移到主队列。status 字段的特殊值用于标记节点所在的队列。
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     * 翻译：感谢 Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer 和 Michael Scott 以及 JSR-166 专家组的成员，他们对本类的设计提出了有益的想法、讨论和评论。
     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        /** 表明节点在共享模式下等待的标记 */
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        /** 表明节点在独占模式下等待的标记 */
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        /** 表明线程已取消的 waitStatus 值 */
        static final int CANCELLED =  1;
        /** waitStatus value to indicate successor's thread needs unparking */
        /** 表明后继线程需要唤醒的 waitStatus 值 */
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        /** 表明线程在 condition 中等待的 waitStatus 值 */
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         * 表明下一次 acquireShared 应无条件传播的 waitStatus 值
         */
        static final int PROPAGATE = -3;

        /**
         * Status field, taking on only the values:
         * 翻译：状态字段，仅采用以下值：
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *               翻译：该节点的后继节点被（或即将被）阻塞（通过 park），因此当前节点在释放或取消时必须唤醒其后继节点。为了避免竞争，acquire 方法必须首先表明它们需要一个信号，然后重试原子获取，然后在获取失败时阻塞。
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *               翻译：由于超时或中断，该节点被取消。节点永远不会离开这个状态。特别是，取消节点的线程永远不会再次阻塞。
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *               翻译：该节点当前在 condition queue 中。直到转移后，它才会用作 sync queue 的节点，此时状态将设置为 0。（此处使用此值与该字段的其他用途无关，但简化了机制。）
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *               翻译：releaseShared 应该传播到其他节点。该状态在 doReleaseShared 中设置（仅适用于头节点）以确保传播继续，即使其它操作已经介入。
         *   0:          None of the above
         *               翻译：以上都不是
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         * 翻译：这些值按数字排列以简化使用。非负值意味着节点不需要发信号。因此，大多数代码不需要检查特定值，只需检查符号。
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         * 翻译：对于普通同步节点，该字段被初始化为 0，对于条件节点，该字段被初始化为 CONDITION。它使用 CAS 修改（或在可能的情况下，无条件 volatile 写入）。
         */
        volatile int waitStatus;

        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         * 翻译：链接到 当前 节点/线程 依赖于检查 waitStatus 的 前驱节点。在入队期间分配，并仅在出队时为空（为了 GC）。此外，在取消前驱时，我们在找到一个未取消的节点时进行短路，这将始终存在，因为头节点永远不会被取消：只有在获取成功后节点才成为 head 节点。一个被取消的线程永远不会成功获取，并且一个线程只会取消自己，而不是任何其他节点。
         */
        volatile Node prev;

        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         * 翻译：链接到 当前 节点/线程 在释放时唤醒的 后继节点。在入队期间分配，在绕过取消的前驱时进行调整，并在出队时为空（为了 GC）。 enq 操作直到连接（attachment）后才分配前驱的 next 字段，因此看到 null next 字段并不一定意味着该节点位于队列末尾。但是，如果下一个字段显示为空，我们可以从 tail 扫描 prev 以进行再次检查。取消节点的下一个字段设置为指向节点本身而不是 null，以使 isOnSyncQueue 的工作更轻松。
         */
        volatile Node next;

        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         * 翻译：入队该节点的线程。在构造时初始化并在使用后置为 null。
         */
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         * 翻译：链接到下一个在 condition 中等待的节点，或特殊值 SHARED。因为 condition queue 只有在独占模式下持有才会被访问，所以我们只需要一个简单的链接队列来保存节点，因为它们在 condition 中等待。然后将它们转移到队列以重新获取。并且因为条件只能是独占的，所以我们保存一个字段通过使用特殊值来表明是共享模式。
         *
         * 如果是共享模式：特殊值 SHARED
         * 如果是独占模式：null 或 下一个在 condition queue 中等待的节点（如果用到 condition）
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         * 翻译：如果节点在共享模式下等待，则返回 true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         * 翻译：返回前驱节点，如果为 null 则抛出 NullPointerException。当前驱不为空时使用。可以省略空检查，但存在可以帮助 VM。
         *
         * @return the predecessor of this node 此节点的前驱
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker 用于建立 dummyHead 或 SHARED 标记
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter （addWaiter）方法中使用
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition （Condition）中使用（准确的说是在 ConditionObject#addConditionWaiter 方法中使用）
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     * 翻译：等待队列的 head，延迟初始化。除初始化外，只能通过 setHead 方法进行修改。注意：如果 head 存在，则其 waitStatus 肯定不为 CANCELLED。
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     * 翻译：等待队列的 tail，延迟初始化。只能通过 enq 方法修改以添加新的等待节点。
     */
    private transient volatile Node tail;

    /**
     * The synchronization state.
     * 翻译：同步状态。
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * 翻译：返回同步状态的当前值。此操作具有 volatile 读的内存语义。
     * @return current state value 当前状态值
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * 翻译：设置同步状态的值。此操作具有 volatile 写的内存语义。
     * @param newState the new state value 新状态值
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     * 翻译：如果当前状态值等于期望值，则原子地将同步状态设置为给定的更新值。此操作具有 volatile 读写的内存语义。
     *
     * @param expect the expected value 期望值
     * @param update the new value 新值
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     *         如果成功，则返回 true。返回 false 表示实际值不等于期望值。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        // 参阅下面的内在设置以支持此功能
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities
    // 排队工具

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     * 翻译：纳秒数，使用自旋而不是定时挂起，这样可以更快响应。粗略估计足以在非常短的超时时间内提高响应能力。
     *
     * 以下方法会调用：
     * doAcquireNanos / doAcquireSharedNanos
     * awaitNanos / await
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * 翻译：将节点插入队列，必要时初始化。见上面 Node 类注释里的图。
     * @param node the node to insert 要插入的节点
     * @return node's predecessor （node）的前驱
     */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize 必须初始化
                // 初始化 dummyHead
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                // 先设置 node.prev = tail，再 CAS 设置 tail = node，最后设置 tail.next = node（注意这不是一个原子操作）
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * Creates and enqueues node for current thread and given mode.
     * 翻译：为当前线程和给定模式创建节点以及入队。
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared （Node.EXCLUSIVE）为独占，Node.SHARED 为共享
     * @return the new node 新节点
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 尝试 enq 方法的 fast path；失败时备份到完整的 enq（失败时走完整的 enq 方法逻辑作为兜底）
        Node pred = tail;
        if (pred != null) {
            // 先设置 node.prev = tail，再 CAS 设置 tail = node，最后设置 tail.next = node（注意这不是一个原子操作）
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        /* 两种情况会执行到这里：1) 队列为空；2) CAS 失败 */
        enq(node);
        return node;
    }

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     * 翻译：将 node 设置为队列 head，从而出队。仅由 acquire 方法调用。为了 GC 和抑制不必要的信号和遍历，还将未使用的字段置为 null。（head 的 thread 和 prev 都为 null）
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * Wakes up node's successor, if one exists.
     * 翻译：唤醒 node 的后继节点（如果存在）。
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         * 翻译：如果状态为负（即可能需要信号），尝试清除预期的信号。如果此操作失败或状态被等待线程更改，也没关系。
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         * 翻译：要 unpark 的线程保留在后继节点中，通常是下一个节点。但如果下一个节点被取消或为 null，则从 tail 向前遍历以找到实际未取消的后继节点。
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        // s 为 node 后第一个状态不为 CANCELLED 的节点
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     * 翻译：共享模式下的释放操作 -- 给后继节点发信号并确保传播。（注意：对于独占模式，如果需要信号，释放相当于调用 head 的 unparkSuccessor 方法。）
     *
     * 以下方法会调用：
     * setHeadAndPropagate
     * releaseShared
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         * 翻译：确保 release 传播，即使还有其他正在进行的 获取/释放。如果需要信号，就以 尝试调用 head 的 unparkSuccessor 方法的 通常方式进行。但如果不需要信号，则设置状态为 PROPAGATE 以确保在 release 时继续传播。此外，我们必须循环以防我们在执行此操作时添加了新节点。此外，与 unparkSuccessor 的其他用途不同，我们需要知道 CAS 重置状态是否失败，如果失败则重新检查。
         */
        for (;;) {
            Node h = head;
            // 说明同步队列中至少有两个节点
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                // 需要唤醒后继节点
                if (ws == Node.SIGNAL) {
                    /* 多条线程可能会同时执行到这里 */
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    /* 但 CAS 保证只会有一条线程执行到这里 */
                    unparkSuccessor(h);
                }
                // 头节点为 0 && CAS 失败
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    // CAS 失败说明：h.waitStatus 由 0 变为了 SIGNAL（新节点入队）或 PROPAGATE（多线程竞争 CAS 导致失败）
                    continue;                // loop on failed CAS
            }
            // 执行到这里，头节点仍未改变（因为共享模式下头节点很容易改变），就退出循环（注意：这是唯一退出循环的方式）
            if (h == head)                   // loop if head changed 如果 head 改变，继续循环
                break;
        }
    }

    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     * 翻译：设置队列头，并检查后继节点是否在共享模式下等待，如果是，而且 propagate > 0 或设置了 PROPAGATE 状态，则继续传播。
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared （tryAcquireShared）方法的返回值
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below 记录 old head 以供下面的检查
        // 将当前节点设置为 head
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         * 如果出现以下情况，尝试向下一个排队节点发出信号：
         *   调用者或之前的操作记录（在 setHead 之前或之后记录 h.waitStatus）表明需要继续传播，
         *     （注意：这里使用 waitStatus 的符号检查，因为 PROPAGATE 状态可能被转换为 SIGNAL。）
         * 并且
         *   node 的下一个节点在共享模式下等待，或者为 null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         * 翻译：这两种检查的保守性可能会导致不必要的唤醒，但只有在有多个线程竞争 获取/释放 时才会如此，所以大多数情况下现在或很快就需要信号。
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            // node 的下一个节点为 null || 在共享模式下等待
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    /**
     * Cancels an ongoing attempt to acquire.
     * 翻译：取消正在进行的获取尝试。
     *
     * @param node the node
     *
     * 以下方法会调用：
     * acquireQueued / doAcquireInterruptibly / doAcquireNanos
     * doAcquireShared / doAcquireSharedInterruptibly / doAcquireSharedNanos
     */
    private void cancelAcquire(Node node) {
        // 独占非公平锁可能会满足该条件
        // Ignore if node doesn't exist 如果节点为 null 则忽略
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors 跳过已取消的前驱
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        // predNext 明显是要断开的节点。如果没有，下面的 CAS 将失败，在这种情况下，我们输掉了 与另一个 cancel 或 signal 的 竞争，因此无需进一步行动。
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // 可以在这里使用无条件写入而不是 CAS。在这个原子步骤之后，其他节点可以跳过我们。之前，我们不受其他线程的干扰。
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves. 如果 node 是 tail，删除 node。
        // 如果 node 是 tail，CAS 设置 tail 指向 pred，成功后继续 CAS 设置 pred.next 为 null
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            // 如果后继需要信号，尝试设置 pred 的 next（为 node.next），这样它将会得到信号。否则唤醒它传播。
            /* 执行到这里说明：node 不是 tail；或者 node 本来是 tail，但是在执行设置 pred 为 tail 操作时，有了新的节点入队，导致 node 不是 tail 了 */
            int ws;
            /**
             * else 逻辑如下：
             * 如果同时满足：
             * 1) node 不是第一个排队的线程：pred 不是 head
             * 2) pred 的状态为 SIGNAL 或 CAS 设置 pred 的状态为 SIGNAL 成功：（pred 的状态是 SIGNAL）或（pred 的状态是 PROPAGATE 或 0 但是 CAS 设置为 SIGNAL 成功）
             * 3) pred 中的线程不为空
             * 则继续判断 node 是否有后继节点（next != null） 且 waitStatus 不是 CANCELLED（next.waitStatus <= 0），如果仍然满足
             * 则 CAS 设置 pred.next 为 node.next
             * 否则，去唤醒后继节点
             */
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    // CAS 设置 pred.next 为 node.next
                    compareAndSetNext(pred, predNext, next);
            } else {
                // 唤醒 node 的后继节点
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     * 翻译：检查和更新获取失败节点的状态。如果线程应该阻塞，则返回 true。这是所有 acquire 循环中的主要信号控制。要求 pred == node.prev。
     *
     * @param pred node's predecessor holding status 持有状态的 node 的前驱
     * @param node the node
     * @return {@code true} if thread should block 如果线程应该阻塞，则返回 true
     *
     * 该方法返回 true 的条件：当 node 的前驱节点 pred 的 waitStatus 是 SIGNAL
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        // ws 是 node 的前驱 pred 的状态
        if (ws == Node.SIGNAL)
            // ws 是 SIGNAL
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * 翻译：node 节点已经给 pred 设置了 SIGNAL 状态，要求 pred 节点 release 时给 node 发信号，所以 node 可以安全地挂起。
             */
            // 直接返回 true
            return true;
        if (ws > 0) {
            // ws > 0 即 ws 是 CANCELLED
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             * 翻译：node 的前驱 pred 被取消。跳过前驱并指示重试。
             */
            // 从 node 开始往前找，直到找到一个状态不为 CANCELLED 的节点，维护 node 和该节点的双向链表关系：将 node.prev 指向 pred，pred.next 指向 node
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // ws 既不是 SIGNAL，也不是 CANCELLED（肯定也不会是 CONDITION）。那就只能是 0 或 PROPAGATE
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             * 翻译：waitStatus 肯定是 0 或 PROPAGATE。表明我们需要一个信号，但还不能挂起。调用者将需要重试以确保其无法在挂起前获取。
             */
            // 将 ws 设置为 SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     * 翻译：中断当前线程的便捷方法。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     * 翻译：挂起然后检查是否中断的便捷方法
     *
     * @return {@code true} if interrupted 如果中断，则返回 true
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        // interrupted() 方法：如果当前线程未被中断，永远返回 false；如果当前线程被中断，重置中断状态，第一次返回 true，后面永远都返回 false
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     * 翻译：各种风格的获取，在 独占/共享 和控制模式中各不相同。每个都差不多，但又有令人讨厌的不同。由于异常机制（包括确保我们在 tryAcquire 抛出异常时取消）和其他控制的相互作用，可能只能进行一点分解，至少在不会对性能造成太大影响的情况下。
     */

    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     * 翻译：已在队列中的线程以独占不间断模式获取。由 条件等待方法 以及 acquire 方法 使用。
     *
     * @param node the node
     * @param arg the acquire argument （acquire）参数
     * @return {@code true} if interrupted while waiting 如果在等待时中断，则返回 true
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            // 不响应中断，但用 interrupted 来标记获取锁后的线程是否被中断，也是此方法最后的返回值
            boolean interrupted = false;
            for (;;) {
                // p 是 node 的前驱节点
                final Node p = node.predecessor();
                // 当 p 是 head，说明 node 就是队列中第一个排队的线程，再次 tryAcquire
                if (p == head && tryAcquire(arg)) {
                    /**
                     * 在这里，node 节点的状态有两种情况：
                     * 1) 0：当队列此时为 head <=> node，该节点还未执行 shouldParkAfterFailedAcquire 方法，直接执行到这里
                     * 2) SIGNAL：该节点已执行过 shouldParkAfterFailedAcquire 方法并挂起，由前置线程释放锁后唤醒
                     */
                    // 获取成功，将当前节点设置为 head
                    setHead(node);
                    // 断开 p（old head），方便垃圾回收
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                // 不是第一个排队的线程 或 再次 tryAcquire 失败
                // shouldParkAfterFailedAcquire 方法判断是否可以将线程挂起（判断 p 的 waitStatus 是否为 SIGNAL，如果不是，就将其 CAS 设置为 SIGNAL）
                // 如果可以挂起线程，调用 parkAndCheckInterrupt 方法挂起
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 当前线程在等待过程中被中断，但是被重置了中断状态，用 interrupted 做标记，等该线程竞争到锁退出循环后再去中断
                    interrupted = true;
            }
        } finally {
            /* 有两种情况会执行到这里：1) 获取到锁；2) tryAcquire(arg) 方法抛出异常（因为是用户自己实现的） */
            if (failed)
                // 只有 2) 会执行到这里
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * 翻译：以独占可中断模式获取。
     * @param arg the acquire argument （acquire）参数
     *
     * 把 addWaiter(Node.EXCLUSIVE) 方法揉进了 acquireQueued 方法里，并做了中断判断
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        // 创建节点并入队
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive timed mode.
     * 翻译：以独占定时模式获取
     *
     * @param arg the acquire argument （acquire）参数
     * @param nanosTimeout max wait time 最长等待时间
     * @return {@code true} if acquired 如果获取到，则返回 true
     *
     * 把 addWaiter(Node.EXCLUSIVE) 方法揉进了 acquireQueued 方法里，并做了超时和中断判断
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        // 计算等待终止时间
        final long deadline = System.nanoTime() + nanosTimeout;
        // 创建节点并入队
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                // 被唤醒后未获取成功，则重新计算最长等待时间
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    // 注意 这里最终也是会执行 cancelAcquire 方法的
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared uninterruptible mode.
     * 翻译：以共享不间断模式获取。
     * @param arg the acquire argument 获取参数
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            // 不响应中断，但用 interrupted 来标记获取锁后的线程是否被中断
            boolean interrupted = false;
            for (;;) {
                // p 是 node 的前驱节点
                final Node p = node.predecessor();
                // 当 p 是 head，说明 node 就是队列中第一个排队的线程
                if (p == head) {
                    // 当 node 就是队列中第一个排队的线程，再次 tryAcquireShared
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        /**
                         * 在这里，node 节点的状态有两种情况：
                         * 1) 0：当队列此时为 head <=> node，该节点还未执行 shouldParkAfterFailedAcquire 方法，直接执行到这里
                         * 2) SIGNAL：该节点已执行过 shouldParkAfterFailedAcquire 方法并挂起，由前置线程释放锁后唤醒
                         */
                        // 到这里说明获取成功
                        setHeadAndPropagate(node, r);
                        // 断开 p（old head），方便垃圾回收
                        p.next = null; // help GC
                        if (interrupted)
                            // 当前线程在等待过程中被中断，但是被重置了中断状态，在这里补上中断
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                // 不是第一个排队的线程 或 再次 tryAcquireShared 失败
                // shouldParkAfterFailedAcquire 方法判断是否可以将线程挂起（判断 p 的 waitStatus 是否为 SIGNAL，如果不是，就将其 CAS 设置为 SIGNAL）
                // 如果可以挂起线程，调用 parkAndCheckInterrupt 方法挂起
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 当前线程在等待过程中被中断，但是被重置了中断状态，用 interrupted 做标记，等该线程竞争到锁后再去中断
                    interrupted = true;
            }
        } finally {
            /* 有两种情况会执行到这里：1) 获取到锁；2) tryAcquireShared(arg) 方法抛出异常（因为是用户自己实现的） */
            if (failed)
                // 只有 2) 会执行到这里
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * 翻译：以共享可中断模式获取。
     * @param arg the acquire argument 获取参数
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        // 创建节点并入队
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 当前线程在等待过程中被中断，且被重置了中断状态，直接抛出异常
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     * 翻译：以共享定时模式获取
     *
     * @param arg the acquire argument 获取参数
     * @param nanosTimeout max wait time 最长等待时间
     * @return {@code true} if acquired 如果获取成功，则返回 true
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        // 计算等待终止时间
        final long deadline = System.nanoTime() + nanosTimeout;
        // 创建节点并入队
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                // 被唤醒后未获取成功，则重新计算最长等待时间
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    // 注意 这里最终也是会执行 cancelAcquire 方法的
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    // 当前线程被中断，直接抛出异常
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     * 翻译：尝试以独占模式获取。该方法应该查询对象的状态是否允许以独占模式获取它，如果允许则获取它。
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     * 翻译：此方法始终由执行 acquire 的线程调用。如果此方法报告失败，acquire 方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。这可用于实现方法 Lock#tryLock()。
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     * 翻译：默认实现抛出 UnsupportedOperationException。
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *            acquire 参数。该值始终是传递给 acquire 方法的值，或者是在进入条件等待时保存的值。无法解释该值，可以代表任何你喜欢的东西。
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     *         如果成功，则返回 true。该对象在成功后被获取。
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果 acquire 会使这个同步器置于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws UnsupportedOperationException if exclusive mode is not supported 如果不支持独占模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     * 翻译：尝试设置（或修改）状态以反映独占模式下的 release。
     *
     * <p>This method is always invoked by the thread performing release.
     * 翻译：此方法始终由执行 release 的线程调用。
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     * 翻译：默认实现抛出 UnsupportedOperationException。
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *            release 参数。该值始终是传递给 release 方法的值，或者是在进入条件等待时的当前状态值。无法解释该值，可以代表任何你喜欢的东西。
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     *         如果此对象现在处于完全 release 状态，以便任何等待线程都可以尝试获取，则返回 true；否则返回 false
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果 release 会使这个同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws UnsupportedOperationException if exclusive mode is not supported 如果不支持独占模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     * 翻译：尝试以共享模式获取。该方法应该查询对象的状态是否允许以共享模式获取它，如果允许则获取它。
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     * 翻译：此方法始终由执行获取的线程调用。如果此方法报告失败，获取方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     * 翻译：默认实现抛出 UnsupportedOperationException。
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *            获取参数。该值始终是传递给获取方法的值，或者是在进入条件等待时保存的值。无法解释该值，可以代表任何你喜欢的东西。
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     *         失败时，返回负值；如果在共享模式下获取成功但后续的共享模式获取不能成功，则返回 0；如果在共享模式下获取成功并且后续的共享模式获取也可能成功，则返回正值，在这种情况下，后续等待线程必须检查可用性。（对三种不同返回值的支持，使该方法可以在 仅有时是独占获取 的上下文中使用。）成功后，该对象被获取。
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果获取会使这个同步器置于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws UnsupportedOperationException if shared mode is not supported 如果不支持共享模式，则抛出 UnsupportedOperationException
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     * 翻译：尝试设置状态以反映共享模式下的释放。
     *
     * <p>This method is always invoked by the thread performing release.
     * 翻译：此方法始终由执行释放的线程调用。
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     * 翻译：默认实现抛出 UnsupportedOperationException。
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *            释放参数。该值始终是传递给 release 方法的值，或者是在进入条件等待时的当前状态值。无法解释该值，可以代表任何你喜欢的东西。
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     *         如果此共享模式的释放可能允许正在等待的线程获取（共享或独占）成功，则返回 true；否则返回 false。
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果释放会使这个同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws UnsupportedOperationException if shared mode is not supported 如果不支持共享模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     * 翻译：如果同步 对于当前（调用）线程 是独占的，则返回 true。每次调用非等待 ConditionObject 方法时都会调用此方法。（等待方法调用 release）
     * with respect to：关于；至于
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     * 翻译：默认实现抛出 UnsupportedOperationException。此方法仅在 ConditionObject 方法内部调用，因此如果不使用 condition 则无需定义。
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     *         如果同步是独占的，则返回 true；否则返回 false
     * @throws UnsupportedOperationException if conditions are not supported 如果不支持 condition，则抛出 UnsupportedOperationException
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     * 翻译：以独占模式获取锁，忽略中断。通过至少调用一次 tryAcquire 方法实现，成功则返回。失败则线程排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquire 方法成功。该方法可用于实现 Lock#lock 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *            acquire 参数。这个值被传递到 #tryAcquire，但无法解释，可以代表任何你喜欢的东西。
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 当前线程在等待过程中被中断，但是被重置了中断状态，在这里补上中断
            selfInterrupt();
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     * 翻译：独占模式获取，如果中断则中止。通过 先检查中断状态，然后至少调用一次 tryAcquire 方法，成功则返回 来实现。否则，线程将排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquire 方法获取成功或线程被中断。该方法可用于实现 Lock#lockInterruptibly 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *            acquire 参数。这个值被传递到 #tryAcquire，但无法解释，可以代表任何你喜欢的东西。
     * @throws InterruptedException if the current thread is interrupted 如果当前线程被中断，则抛出 InterruptedException
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            // 以独占可中断模式获取。其实就是把 addWaiter(Node.EXCLUSIVE) 方法揉进了 acquireQueued 方法里，并做了中断判断
            doAcquireInterruptibly(arg);
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     * 翻译：尝试以独占模式获取，如果中断则中止，如果超过给定的超时时间则失败。通过 先检查中断状态，然后至少调用一次 tryAcquire 方法，成功则返回 来实现。否则，线程将排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquire 方法获取成功 或 线程被中断 或 超时。该方法可用于实现 Lock#tryLock(long, TimeUnit) 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *            acquire 参数。这个值被传递到 #tryAcquire，但无法解释，可以代表任何你喜欢的东西。
     * @param nanosTimeout the maximum number of nanoseconds to wait 等待的最大纳秒数
     * @return {@code true} if acquired; {@code false} if timed out 如果获取到，则返回 true；如果超时，则返回 false
     * @throws InterruptedException if the current thread is interrupted 如果当前线程被中断，则抛出 InterruptedException
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
                // 以独占定时模式获取。其实就是把 addWaiter(Node.EXCLUSIVE) 方法揉进了 acquireQueued 方法里，并做了超时和中断判断
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     * 翻译：独占模式下 release。如果 tryRelease 方法返回 true，则通过解除阻塞一个或多个线程来实现。该方法可用于实现 Lock#unlock 方法。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     *            release 参数。这个值被传递到 #tryRelease，但无法解释，可以代表任何你喜欢的东西。
     * @return the value returned from {@link #tryRelease} （tryRelease）方法的返回值
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            // 当 h.waitStatus == 0 说明 sync queue 中此时只有一个 head 节点 或 有其它线程正在执行 unparkSuccessor 方法
            if (h != null && h.waitStatus != 0)
                // 唤醒 head 的后继节点
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     * 翻译：以共享模式获取，忽略中断。通过首先至少调用一次 tryAcquireShared 方法来实现，成功则返回。失败则线程排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquireShared 方法成功。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *            获取参数。这个值被传递到 tryAcquireShared 方法，但无法解释该值，可以代表任何你喜欢的东西。
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            // 获取锁失败
            doAcquireShared(arg);
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * 翻译：共享模式获取，如果中断则中止。通过 先检查中断状态，然后至少调用一次 tryAcquireShared 方法，成功则返回 来实现。否则，线程将排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquireShared 方法获取成功或线程被中断。
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     *            获取参数。该值被传递到 tryAcquire 方法，但无法解释，可以代表任何你喜欢的东西。
     * @throws InterruptedException if the current thread is interrupted 如果当前线程被中断，则抛出 InterruptedException
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            // 以共享可中断模式获取。其实就是在 doAcquireShared 方法的基础上，做了中断判断
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     * 翻译：尝试以共享模式获取，如果中断则中止，如果超过给定的超时时间则失败。通过 先检查中断状态，然后至少调用一次 tryAcquireShared 方法，成功则返回 来实现。否则，线程将排队，可能会反复阻塞和解除阻塞，直到调用 tryAcquireShared 方法获取成功 或 线程被中断 或 超时。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *            获取参数，这个值被传递到 tryAcquireShared 方法，但无法解释，可以代表任何你喜欢的东西。
     * @param nanosTimeout the maximum number of nanoseconds to wait 等待的最大纳秒数
     * @return {@code true} if acquired; {@code false} if timed out 如果获取成功，则返回 true；如果超时，则返回 false
     * @throws InterruptedException if the current thread is interrupted 如果当前线程被中断，则抛出 InterruptedException
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
                // 以共享定时模式获取。其实就是在 doAcquireShared 方法的基础上，做了超时和中断判断
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     * 翻译：共享模式下释放。如果 tryReleaseShared 方法返回 true，则通过解除阻塞一个或多个线程来实现。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *            释放参数，这个值被传递到 tryReleaseShared 方法，但无法解释，可以代表任何你喜欢的东西。
     * @return the value returned from {@link #tryReleaseShared} （tryReleaseShared）方法的返回值
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods
    // 队列检查方法

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     * 翻译：查询是否有任何线程等待获取的时间 比当前线程长。
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     * 翻译：调用此方法等效于（但可能比 下面的代码 更有效）：
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     * 翻译：注意，由于中断和超时导致的取消随时可能发生，因此返回 true 并不能保证其他线程会在当前线程之前获取。同样，由于队列为空，在此方法返回 false 后，另一个线程可能会赢得入队竞争。
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     * 翻译：此方法旨在供公平同步器使用，以避免 插入（barging）。这样一个同步器的 #tryAcquire 方法应该返回 false，并且它的 #tryAcquireShared 方法应该返回一个负值，如果此方法返回 true（除非这是一个可重入获取）。例如，公平、可重入、独占模式同步器的 #tryAcquire 方法可能如下所示：
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     // 可重入获取；增加保留计数
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *     // 尝试正常获取
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     *         如果当前线程前面有一个排队线程，则返回 true；如果当前线程在队列的头部或队列为空，则返回 false
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        // 此操作的正确性取决于 head 在 tail 之前被初始化，以及，如果当前线程是队列中的第一个，head.next 是准确的。
        Node t = tail; // Read fields in reverse initialization order 以相反的初始化顺序读取字段
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * 翻译：如果一个节点（始终最初在 condition queue 中）现在正在 sync queue 中等待重新获取，则返回 true。
     * @param node the node
     * @return true if is reacquiring 如果正在重新获取，则返回 true
     */
    final boolean isOnSyncQueue(Node node) {
        // todo 优化表达
        // 如果节点的 waitStatus 为 CONDITION
        // 如果节点的 waitStatus 不为 CONDITION 且 没有前驱节点
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue 如果有后继节点，肯定是在同步队列中
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         * 翻译：node.prev 可以不为 null 但尚未在队列中，因为将其放入队列的 CAS 操作可能会失败。所以我们必须从尾部遍历以确保它确实成功了。在调用此方法时它总是在 tail 附近，除非 CAS 失败（这不太可能），所以我们几乎不会遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * 翻译：如果节点通过从 tail 向前搜索在 sync queue 中，则返回 true。仅在 isOnSyncQueue 需要时调用。
     * @return true if present 如果存在，则返回 true
     *
     * 只有 isOnSyncQueue() 方法调用
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * 翻译：将节点从 condition queue 转移到 sync queue。如果成功，则返回 true。
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     * 如果成功转移（否则说明节点在获取到信号之前被取消），则返回 true
     *
     * 以下方法会调用：
     * doSignal / doSignalAll
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 翻译：如果 CAS 修改 waitStatus 失败，说明该节点已被取消。
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         * 翻译：node 入同步队列并尝试设置前驱节点的 waitStatus 来表明线程（可能）正在等待。如果（前驱节点）已取消或尝试设置 waitStatus 失败，则唤醒 node 中的线程以重新同步（这种情况下 waitStatus 可能是短暂且无害的错误）。
         */
        // p 是 node 的前驱节点
        Node p = enq(node);
        int ws = p.waitStatus;
        // 已取消 或 CAS 修改 waitStatus 失败
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     * 翻译：如有必要，在取消等待后将节点转移到同步队列。如果线程在 signal 之前被取消，则返回 true。
     *
     * @param node the node
     * @return true if cancelled before the node was signalled 如果在节点被 signal 之前取消，则返回 true
     */
    final boolean transferAfterCancelledWait(Node node) {
        // CAS 成功说明 node 状态为 CONDITION，说明 node 还在 condition queue 中，说明还没有调用 signal()
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 翻译：如果我们输给了一个 signal()（此时 signal() 方法正在执行），那么在它执行完 enq() 前我们都不能继续。在没完全转移期间取消既罕见又短暂，所以只需自旋。
         */
        // 说明已调用 signal()，当 signal() -> doSignal() -> transferForSignal() 中的 enq() 方法执行完才会跳出循环
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * 翻译：使用当前 state 值调用 release 方法；返回保存的状态。失败时取消节点并抛出异常。
     * @param node the condition node for this wait 等待的条件节点
     * @return previous sync state 之前的同步状态
     */
    final int fullyRelease(Node node) {
        // release 失败
        boolean failed = true;
        try {
            // 当前状态值，也是最终返回值（无论重入几次，全部释放）
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                // release 返回 false，即 tryRelease 返回 false
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                // release 失败，当前节点状态改为 CANCELLED
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     * 翻译：AbstractQueuedSynchronizer 的 Condition 实现，作为 Lock 实现的基础。
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     * 翻译：此类的方法文档从 Lock 和 Condition 用户的角度描述了机制，而不是行为规范。此类的导出版本通常需要附有描述 依赖于关联 AbstractQueuedSynchronizer 的 条件语义的文档。
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     * 翻译：此类是可序列化的，但所有字段都是 transient 的，所以反序列化的 conditions 没有 waiters。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        /** condition queue 的第一个节点。 */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        /** condition queue 的最后一个节点。 */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         * 创建一个新的 ConditionObject 实例。
         */
        public ConditionObject() { }

        // Internal methods
        // 内部方法

        /**
         * Adds a new waiter to wait queue.
         * 翻译：添加一个新的 waiter 到等待队列。
         * @return its new wait node 返回新的等待节点
         *
         * 以下方法会调用：（其实都是 await 相关方法）
         * awaitUninterruptibly / await() / awaitNanos / awaitUntil / await(long time, TimeUnit unit)
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            // 翻译：如果 lastWaiter 状态为 CANCELLED，清理一遍 condition queue（即调用 unlinkCancelledWaiters）
            // why？因为 fullyRelease 中如果释放失败会将 waitStatus 置为 CANCELLED
            if (t != null && t.waitStatus != Node.CONDITION) {
                // 断开 condition queue 中所有状态为 CANCELLED 的 waiter 节点
                unlinkCancelledWaiters();
                // t 指向 clean out 后的 lastWaiter
                t = lastWaiter;
            }
            // 将当前线程封装为状态为 CONDITION 的节点
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                /* condition queue 为空 */
                firstWaiter = node;
            else
                /* condition queue 不为空 */
                t.nextWaiter = node;
            // 更新 lastWaiter
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * 翻译：移除并转移节点，直到命中未取消的节点或空。从信号中分离出来部分是为了鼓励编译器内联没有 waiters 的情况。
         * @param first (non-null) the first node on condition queue
         *              条件队列中的第一个节点（不能为空）
         *
         * 只有 signal() 方法调用
         */
        private void doSignal(Node first) {
            do {
                // 判断条件队列中是否只有一个节点（first.next == null）
                if ( (firstWaiter = first.nextWaiter) == null)
                    // 条件队列中只有一个节点，更新 lastWaiter
                    lastWaiter = null;
                // 断开 first
                first.nextWaiter = null;
                    // 转移 first。如果转移成功则执行结束；如果转移失败（节点已被取消）则去处理下一个节点（如果下一个节点为空则执行结束；否则去转移下一个节点）
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * Removes and transfers all nodes.
         * 翻译：移除并转移所有节点。
         * @param first (non-null) the first node on condition queue
         *              条件队列中的第一个节点（不能为空）
         *
         * 只有 signalAll() 方法调用
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                // 断开 first
                first.nextWaiter = null;
                // 转移 first，不论成功与否
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         * 翻译：从 condition queue 中断开已取消的 waiter 节点。仅在持有锁时调用。当在条件等待期间发生取消，以及 lastWaiter 已被取消时插入新的 waiter，将调用此方法。需要此方法来避免在没有信号的情况下保留垃圾。因此，即使它可能需要完全遍历，它也仅在没有信号的情况下发生超时或取消时才发挥作用。它遍历所有节点而不是在特定目标处停止来断开所有指向垃圾节点的指针，而无需在取消风暴期间进行多次重新遍历。
         *
         * 以下方法会调用：（其实都是 await 相关方法）
         * addConditionWaiter / await() / awaitNanos / awaitUntil / await(long time, TimeUnit unit)
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            // firstWaiter 到 trail 中的所有节点状态都为 CONDITION
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    /*
                    t.nextWaiter 可能为：
                      CANCELLED
                      0：概率很小。transferForSignal 方法会将 waitStatus 改为 0，但在调用该方法前，已经将节点从 condition queue 中断开了，但也有可能，signal 有可能发生在 Node t = firstWaiter 之后，不过无所谓，这个本来就是应该断开的
                     */
                    // 断开 t 和 next
                    t.nextWaiter = null;
                    if (trail == null)
                        // t 为头节点
                        firstWaiter = next;
                    else
                        // t 不为头节点
                        trail.nextWaiter = next;
                    if (next == null)
                        // 遍历结束，更新 lastWaiter
                        lastWaiter = trail;
                }
                else
                    /* t.nextWaiter 为 CONDITION */
                    trail = t;
                t = next;
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         * 将等待最久的线程（如果存在）从等待此条件的队列移动到等待获取锁的队列。
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         *         如果 isHeldExclusively 方法返回 false，则抛出 IllegalMonitorStateException
         */
        public final void signal() {
            // 如果同步不是独占的，则抛出 IllegalMonitorStateException
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                // 移除并转移 first
                doSignal(first);
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         * 翻译：将所有线程从该条件的等待队列移动到拥有锁的等待队列
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         *         如果 isHeldExclusively 方法返回 false，则抛出 IllegalMonitorStateException
         */
        public final void signalAll() {
            // 持有锁的不是当前线程，则抛出 IllegalMonitorStateException
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                // 移除并转移所有节点
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        /** 退出等待时重新中断 */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        /** 退出等待时抛出 InterruptedException */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         * 翻译：检查中断，如果在 signal 之前中断，则返回 THROW_IE，如果在 signal 之后中断，则返回 REINTERRUPT，如果没有中断，则返回 0。
         */
        private int checkInterruptWhileWaiting(Node node) {
            // interrupted() 方法：如果当前线程未被中断，永远返回 false；如果当前线程被中断，重置中断状态，第一次返回 true，后面永远都返回 false
            return Thread.interrupted() ?
                    // 有中断，判断被中断唤醒时有没有调用 signal
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    // 没有中断
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         * 翻译：实现可中断的条件等待。
         * 1) 如果当前线程被中断，则抛出 InterruptedException。
         * 2) 保存 getState 方法返回的锁状态。
         * 3) 使用保存的状态作为参数调用 release 方法，如果失败，则抛出 IllegalMonitorStateException。
         * 4) 阻塞直到调用 signal 或被中断。
         * 5) 通过使用保存的状态作为参数调用专用版本的获取方法（acquireQueued() 方法）来重新获取。
         * 6) 如果在步骤 4 中阻塞时被中断，则抛出 InterruptedException。
         */
        public final void await() throws InterruptedException {
            // 1)
            if (Thread.interrupted())
                throw new InterruptedException();
            // 将当前线程封装成状态为 CONDITION 的 Node 并添加到条件队列
            Node node = addConditionWaiter();
            // 2) and 3)
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            // 节点不在 sync queue 中
            while (!isOnSyncQueue(node)) {
                // 4)
                LockSupport.park(this);
                // checkInterruptWhileWaiting 方法判断是什么原因唤醒当前线程的？
                // 0：没被中断过，正常调用 signal 唤醒
                // THROW_IE：被中断唤醒（还没调用 signal）
                // REINTERRUPT：先调用 signal（还没被唤醒），然后被中断唤醒
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            // 5)
            // 什么情况下为 REINTERRUPT？（最后要补上中断）
            // acquireQueued(node, savedState) && interruptMode == 0：获取到锁的过程中被中断 && 在 condition queue 中没被中断过，是正常调用 signal 唤醒
            // acquireQueued(node, savedState) && interruptMode == REINTERRUPT：获取到锁的过程中被中断 && 在 condition queue 中先调用 signal（还没被唤醒），然后被中断唤醒
            // !acquireQueued(node, savedState) && interruptMode == REINTERRUPT：获取到锁的过程中没被中断 && 在 condition queue 中先调用 signal（还没被唤醒），然后被中断唤醒的
            // 2 和 3 可合并为：只要 interruptMode == REINTERRUPT，那么不用关心 acquireQueued 返回 true 或 false。所以这个条件其实是处理 1 的
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                // 当前节点获取到锁并成为头节点了，将此节点从 condition queue 中断开。这个节点可以是 condition queue 中的任意一个节点！
                unlinkCancelledWaiters();
            // 中断唤醒的，根据 interruptMode 处理中断
            if (interruptMode != 0)
                // 6)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            // 持有锁的不是当前线程，则抛出 IllegalMonitorStateException
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            // 持有锁的不是当前线程，则抛出 IllegalMonitorStateException
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            // 持有锁的不是当前线程，则抛出 IllegalMonitorStateException
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
