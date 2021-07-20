/*
 * Copyright (c) 1994, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.security.util.SecurityConstants;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;


/**
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 * 翻译：thread 是程序中的执行线程。JVM 允许应用程序同时运行多个执行线程。
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority. Each thread
 * may or may not also be marked as a daemon. When code running in
 * some thread creates a new <code>Thread</code> object, the new
 * thread has its priority initially set equal to the priority of the
 * creating thread, and is a daemon thread if and only if the
 * creating thread is a daemon.
 * 翻译：每个线程都有一个优先级。优先级高的线程优先于优先级低的线程执行。每个线程可能被标记为或者不被标记为守护线程。当在某些线程运行的代码中创建一个新的 Thread 对象时，新线程的优先级最初设置为与创建线程的优先级相等，并且只有当创建线程是守护线程时，新线程才是守护线程。
 * <p>
 * When a Java Virtual Machine starts up, there is usually a single
 * non-daemon thread (which typically calls the method named
 * <code>main</code> of some designated class). The Java Virtual
 * Machine continues to execute threads until either of the following
 * occurs:
 * 翻译：当 JVM 启动时，通常会有一个非守护线程（通常是调用某些指定类的 main 方法）。JVM 会继续执行线程，直到发生以下任一情况：
 * <ul>
 * <li>The <code>exit</code> method of class <code>Runtime</code> has been
 *     called and the security manager has permitted the exit operation
 *     to take place.
 *     翻译：Runtime 类的 exit 方法被调用，且安全管理器已允许退出操作的发生。
 * <li>All threads that are not daemon threads have died, either by
 *     returning from the call to the <code>run</code> method or by
 *     throwing an exception that propagates beyond the <code>run</code>
 *     method.
 *     翻译：所有不是守护线程的线程都已经死亡，要么是调用 run 方法后返回，要么是通过抛出传播到 run 方法之外的异常。
 * </ul>
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of <code>Thread</code>. This
 * subclass should override the <code>run</code> method of class
 * <code>Thread</code>. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * 翻译：有两种方法可以创建新的执行线程。一种是将一个类声明为 Thread 的子类。这个子类应该重写 Thread 类的 run 方法。然后可以分配和启动子类的实例。例如，计算大于规定值的素数（质数）的线程可以写成如下：
 * <hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * 翻译：下面的代码将创建一个线程并启动运行它：
 * <blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the <code>Runnable</code> interface. That class then
 * implements the <code>run</code> method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * <code>Thread</code>, and started. The same example in this other
 * style looks like the following:
 * 翻译：创建线程的另一种方法是声明一个实现 Runnable 接口的类。然后该类实现 run 方法。然后可以分配该类的实例，在创建 Thread 时将其作为参数传递并启动。上面相同例子的另一种写法如下所示：
 * <hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * 翻译：下面的代码将创建一个线程并启动运行它：
 * <blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 * Every thread has a name for identification purposes. More than
 * one thread may have the same name. If a name is not specified when
 * a thread is created, a new name is generated for it.
 * 翻译：每个线程都有一个用于识别的名称。多个线程可能具有相同的名称。如果在创建线程时未指定名称，则会为其生成一个新名称。
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 * 翻译：除非另有说明，否则将 null 参数传递给该类中的构造函数或方法将导致抛出 NullPointerException。
 *
 * @author  unascribed
 * @see     Runnable
 * @see     Runtime#exit(int)
 * @see     #run()
 * @see     #stop()
 * @since   JDK1.0
 */
public
class Thread implements Runnable {
    /* Make sure registerNatives is the first thing <clinit> does. */
    /* 确保 registerNatives 方法是 <clinit> 做的第一件事 */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
     * 名称
     */
    private volatile String name;
    /**
     * 优先级
     */
    private int            priority;
    private Thread         threadQ;
    private long           eetop;

    /* Whether or not to single_step this thread. */
    /* 是否单步执行（single_step）此线程。 */
    private boolean     single_step;

    /* Whether or not the thread is a daemon thread. */
    /* 当前线程是否为守护线程。 */
    private boolean     daemon = false;

    /* JVM state */
    /* JVM 状态 */
    private boolean     stillborn = false;

    /* What will be run. */
    /* 运行目标：new Thread 时传入的 Runnable 参数 */
    private Runnable target;

    /* The group of this thread */
    /* 当前线程的组 */
    private ThreadGroup group;

    /* The context ClassLoader for this thread */
    /* 当前线程的上下文类加载器 */
    private ClassLoader contextClassLoader;

    /* The inherited AccessControlContext of this thread */
    /* 当前线程继承的 AccessControlContext */
    private AccessControlContext inheritedAccessControlContext;

    /* For autonumbering anonymous threads. */
    /* 用于给匿名线程自动编号 */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class.
     * 与当前线程有关的 ThreadLocal 值。该 map 由 ThreadLocal 类维护。
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;

    /*
     * InheritableThreadLocal values pertaining to this thread. This map is
     * maintained by the InheritableThreadLocal class.
     * 与当前线程有关的 InheritableThreadLocal 值。该 map 由 InheritableThreadLocal 类维护。
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    /*
     * The requested stack size for this thread, or 0 if the creator did
     * not specify a stack size.  It is up to the VM to do whatever it
     * likes with this number; some VMs will ignore it.
     * 当前线程请求的堆栈大小，如果创建者未指定堆栈大小，则为 0。虚拟机可以用这个数字做任何它喜欢的事情；一些虚拟机会忽略该值。
     */
    private long stackSize;

    /*
     * JVM-private state that persists after native thread termination.
     * 在 native 线程终止后持续存在的 JVM 私有状态。
     */
    private long nativeParkEventPointer;

    /*
     * Thread ID
     * 线程 ID
     */
    private long tid;

    /* For generating thread ID */
    /* 用于生成线程 ID */
    private static long threadSeqNumber;

    /* Java thread status for tools,
     * initialized to indicate thread 'not yet started'
     * 线程状态，初始化为 0 表明线程 '尚未启动'
     */

    private volatile int threadStatus = 0;


    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }

    /**
     * The argument supplied to the current call to
     * java.util.concurrent.locks.LockSupport.park.
     * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
     * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
     * 提供给当前调用 java.util.concurrent.locks.LockSupport.park 的参数。由（私有） java.util.concurrent.locks.LockSupport.setBlocker 设置，使用 java.util.concurrent.locks.LockSupport.getBlocker 访问
     */
    volatile Object parkBlocker;

    /* The object in which this thread is blocked in an interruptible I/O
     * operation, if any.  The blocker's interrupt method should be invoked
     * after setting this thread's interrupt status.
     * 当前线程在可中断 IO 操作中被阻塞的对象（如果有）。设置当前线程的中断状态后，应调用 blocker 的中断方法。
     */
    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    /* Set the blocker field; invoked via sun.misc.SharedSecrets from java.nio code
     */
    /* 设置拦截器字段（blocker field）；通过 sun.misc.SharedSecrets 从 java.nio 代码调用 */
    void blockedOn(Interruptible b) {
        synchronized (blockerLock) {
            blocker = b;
        }
    }

    /**
     * The minimum priority that a thread can have.
     * 线程可以拥有的最低优先级。
     */
    public final static int MIN_PRIORITY = 1;

   /**
     * The default priority that is assigned to a thread.
    * 分配给线程的默认优先级。
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     * 线程可以拥有的最高优先级。
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * Returns a reference to the currently executing thread object.
     * 返回对当前正在执行的线程对象的引用。
     *
     * @return  the currently executing thread.
     */
    public static native Thread currentThread();

    /**
     * A hint to the scheduler that the current thread is willing to yield
     * its current use of a processor. The scheduler is free to ignore this
     * hint.
     * 翻译：向调度程序提示当前线程愿意放弃其当前对处理器的使用。调度程序可以随意忽略此提示。
     *
     * <p> Yield is a heuristic attempt to improve relative progression
     * between threads that would otherwise over-utilise a CPU. Its use
     * should be combined with detailed profiling and benchmarking to
     * ensure that it actually has the desired effect.
     * 翻译：yield 是一种启发式尝试，旨在改善线程之间的相对进展，否则会过度使用 CPU。它的使用应与详细的分析和基准测试相结合，以确保它确实具有预期的效果。
     *
     * <p> It is rarely appropriate to use this method. It may be useful
     * for debugging or testing purposes, where it may help to reproduce
     * bugs due to race conditions. It may also be useful when designing
     * concurrency control constructs such as the ones in the
     * {@link java.util.concurrent.locks} package.
     * 翻译：此方法很少有适合的场景使用。对于调试或测试目的可能很有用，它可能有助于重现由于竞争条件引起的 bugs。在设计并发控制结构（例如 java.util.concurrent.locks 包中的结构）时，它也可能很有用。
     */
    public static native void yield();

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to
     * the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     * 翻译：使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数，取决于系统计时器和调度程序的精度和准确性。该线程不会失去任何监视器的所有权。
     *
     * @param  millis
     *         the length of time to sleep in milliseconds
     *         以毫秒为单位的睡眠时间长度
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
     *          如果 millis 的值为负，则抛出 IllegalArgumentException
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     *          如果任何线程中断了当前线程。抛出 InterruptedException 异常时，当前线程的 中断状态 被清除。
     */
    public static native void sleep(long millis) throws InterruptedException;

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds plus the specified
     * number of nanoseconds, subject to the precision and accuracy of system
     * timers and schedulers. The thread does not lose ownership of any
     * monitors.
     * 翻译：使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数加上指定的纳秒数，取决于系统计时器和调度程序的精度和准确性。该线程不会失去任何监视器的所有权。
     *
     * @param  millis
     *         the length of time to sleep in milliseconds
     *         以毫秒为单位的睡眠时间长度
     *
     * @param  nanos
     *         {@code 0-999999} additional nanoseconds to sleep
     *         0-999999 额外的纳秒睡眠时间
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative, or the value of
     *          {@code nanos} is not in the range {@code 0-999999}
     *          如果 millis 的值为负，或 nanos 的值不在 0-999999 范围内，则抛出 IllegalArgumentException
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     *          如果任何线程中断了当前线程。抛出 InterruptedException 异常时，当前线程的 中断状态 被清除。
     */
    public static void sleep(long millis, int nanos)
    throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        // nanos 的范围为 0-999999 纳秒
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        /**
         * 1s = 1,000ms = 1,000,000μs(微秒) = 1,000,000,000ns = 1,000,000,000,000ps(皮秒)
         * 1ms = 1,000 000ns
         * 500000ns = 0.5ms
         *
         * 有意思在于实现和注释不太相符。。
         * 注释中说，休眠时间 = 毫秒数 + 纳秒数，但其实真实情况是：
         * (1) 当 nanos 小于 0.5ms时，millis 不变；
         * (2) 当 nanos 大于等于 0.5ms时，millis 加 1；
         * (3) 当 nanos 不为 0 且 millis 为 0 时，millis 加 1
         *
         * 也就是说该方法并没有实现 ns 级别的睡眠，最小的睡眠时间为 1ms
         *
         * java.lang.Thread#join(long, int) 中也有这么一段
         */
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    /**
     * Initializes a Thread with the current AccessControlContext.
     * 翻译：用当前 AccessControlContext 初始化一个线程。
     * @see #init(ThreadGroup,Runnable,String,long,AccessControlContext,boolean)
     */
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        init(g, target, name, stackSize, null, true);
    }

    /**
     * Initializes a Thread.
     * 翻译：初始化一个线程。
     *
     * @param g the Thread group 线程组
     * @param target the object whose run() method gets called 其 run 方法被调用的对象
     * @param name the name of the new Thread 新线程的名称
     * @param stackSize the desired stack size for the new thread, or
     *        zero to indicate that this parameter is to be ignored.
     *                  新线程所需的堆栈大小，或为零表示将忽略此参数。
     * @param acc the AccessControlContext to inherit, or
     *            AccessController.getContext() if null
     *            要继承的 AccessControlContext，如果为 null，则为 AccessController.getContext()
     * @param inheritThreadLocals if {@code true}, inherit initial values for
     *            inheritable thread-locals from the constructing thread
     *                            如果为 true，从构造线程继承 可继承的 thread-locals 的初始值
     */
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;

        // 当前正在执行的线程作为新线程的父线程，因为新线程要继承创建它的线程的一些属性
        Thread parent = currentThread();
        // 安全管理器
        SecurityManager security = System.getSecurityManager();
        if (g == null) {
            /* Determine if it's an applet or not */
            /* 确定是否是一个 applet 程序 */

            /* If there is a security manager, ask the security manager
               what to do.
               如果有安全管理器，询问安全管理器要做什么。
               */
            if (security != null) {
                g = security.getThreadGroup();
            }

            /* If the security doesn't have a strong opinion of the matter
               use the parent thread group.
               如果安全管理器对此没有强烈意见，使用父线程组。
               */
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }

        /* checkAccess regardless of whether or not threadgroup is
           explicitly passed in.
           无论是否显式传入线程组都 checkAccess。
           */
        g.checkAccess();

        /*
         * Do we have the required permissions?
         * 我们拥有所需的权限？
         */
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        // group 中的未启动线程数加 1
        g.addUnstarted();

        this.group = g;
        // 继承父线程的守护线程属性
        this.daemon = parent.isDaemon();
        // 继承父线程的优先级属性
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext =
                acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        // 当 inheritThreadLocals 为 true 且父线程的 inheritableThreadLocals 属性值不为空时，把 inheritableThreadLocals 属性的值传递给子线程
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        /* 存储指定的堆栈大小，以防 VM 关心 */
        this.stackSize = stackSize;

        /* Set thread ID */
        /* 设置线程 id：自增 */
        tid = nextThreadID();
    }

    /**
     * Throws CloneNotSupportedException as a Thread can not be meaningfully
     * cloned. Construct a new Thread instead.
     * 翻译：抛出 CloneNotSupportedException，因为克隆线程没意义。如果需要，应该构造一个新线程。
     *
     * @throws  CloneNotSupportedException
     *          always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, null, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果，其中 gname 是一个新生成的名称。自动生成的名称格式为 "Thread-" + n，其中 n 是一个整数。
     */
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, target, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果，其中 gname 是一个新生成的名称。自动生成的名称格式为 "Thread-" + n，其中 n 是一个整数。
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this classes {@code run} method does
     *         nothing.
     *         启动当前线程时调用其 run 方法的对象。如果为 null，则该类的 run 方法什么都不做。
     */
    public Thread(Runnable target) {
        init(null, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * Creates a new Thread that inherits the given AccessControlContext.
     * This is not a public constructor.
     * 翻译：创建一个继承给定 AccessControlContext 的新线程。这不是公共构造函数。
     */
    Thread(Runnable target, AccessControlContext acc) {
        // 如果不传线程名称，则默认线程名称为 "Thread-" + nextThreadNum()
        init(null, target, "Thread-" + nextThreadNum(), 0, acc, false);
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (group, target, gname)} ,where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果，其中 gname 是一个新生成的名称。自动生成的名称格式为 "Thread-" + n，其中 n 是一个整数。
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *         线程组。如果为 null 且有安全管理器，则 group 由 SecurityManager#getThreadGroup 确定。如果没有安全管理器或 SecurityManager.getThreadGroup() 返回 null，则 group 设置为当前线程（创建新线程的线程）的线程组。
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *         启动当前线程时调用其 run 方法的对象。如果为 null，则调用当前线程的 run 方法。
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     *          如果当前线程无法在指定的线程组中创建线程，则抛出 SecurityException
     */
    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, null, name)}.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果。
     *
     * @param   name
     *          the name of the new thread
     */
    public Thread(String name) {
        init(null, null, name, 0);
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (group, null, name)}.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果。
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *         线程组。如果为 null 且有安全管理器，则 group 由 SecurityManager#getThreadGroup 确定。如果没有安全管理器或 SecurityManager.getThreadGroup() 返回 null，则 group 设置为当前线程（创建新线程的线程）的线程组。
     *
     * @param  name
     *         the name of the new thread
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     *          如果当前线程无法在指定的线程组中创建线程，则抛出 SecurityException
     */
    public Thread(ThreadGroup group, String name) {
        init(group, null, name, 0);
    }

    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, target, name)}.
     * 翻译：分配一个新的 Thread 对象。此构造函数与 #Thread(ThreadGroup,Runnable,String) 具有相同的效果。
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *         启动当前线程时调用其 run 方法的对象。如果为 null，则调用当前线程的 run 方法。
     *
     * @param  name
     *         the name of the new thread
     */
    public Thread(Runnable target, String name) {
        init(null, target, name, 0);
    }

    /**
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}.
     * 翻译：分配一个新的 Thread 对象，target 作为其运行对象，name 作为其名称，并且属于 group 所引用的线程组。
     *
     * <p>If there is a security manager, its
     * {@link SecurityManager#checkAccess(ThreadGroup) checkAccess}
     * method is invoked with the ThreadGroup as its argument.
     * 翻译：如果存在安全管理器，则调用其 SecurityManager#checkAccess(ThreadGroup) 方法并使用 ThreadGroup 作为其参数。
     *
     * <p>In addition, its {@code checkPermission} method is invoked with
     * the {@code RuntimePermission("enableContextClassLoaderOverride")}
     * permission when invoked directly or indirectly by the constructor
     * of a subclass which overrides the {@code getContextClassLoader}
     * or {@code setContextClassLoader} methods.
     * 翻译：此外，当 由重写了 getContextClassLoader 或 setContextClassLoader 方法的子类的构造函数 直接或间接调用时，其 checkPermission 方法将使用 RuntimePermission("enableContextClassLoaderOverride") 权限调用。
     *
     * <p>The priority of the newly created thread is set equal to the
     * priority of the thread creating it, that is, the currently running
     * thread. The method {@linkplain #setPriority setPriority} may be
     * used to change the priority to a new value.
     * 翻译：新创建线程的优先级与创建它的线程（即当前运行的线程）的优先级相等。 setPriority 方法可将优先级更改为新值。
     *
     * <p>The newly created thread is initially marked as being a daemon
     * thread if and only if the thread creating it is currently marked
     * as a daemon thread. The method {@linkplain #setDaemon setDaemon}
     * may be used to change whether or not a thread is a daemon.
     * 翻译：当且仅当创建它的线程当前被标记为守护线程时，新创建的线程最初才会被标记为守护线程。setDaemon 方法可更改线程是否为守护线程。
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *         线程组。如果为 null 且有安全管理器，则 group 由 SecurityManager#getThreadGroup 确定。如果没有安全管理器或 SecurityManager.getThreadGroup() 返回 null，则 group 设置为当前线程（创建新线程的线程）的线程组。
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *         启动当前线程时调用其 run 方法的对象。如果为 null，则调用当前线程的 run 方法。
     *
     * @param  name
     *         the name of the new thread
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group or cannot override the context class loader methods.
     *          如果当前线程无法在指定的线程组中创建线程 或 不能重写上下文类加载器方法，则抛出 SecurityException
     */
    public Thread(ThreadGroup group, Runnable target, String name) {
        init(group, target, name, 0);
    }

    /**
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}, and has
     * the specified <i>stack size</i>.
     * 翻译：分配一个新的 Thread 对象，target 作为其运行对象，name 作为其名称，并且属于 group 所引用的线程组，并具有指定的堆栈大小。
     *
     * <p>This constructor is identical to {@link
     * #Thread(ThreadGroup,Runnable,String)} with the exception of the fact
     * that it allows the thread stack size to be specified.  The stack size
     * is the approximate number of bytes of address space that the virtual
     * machine is to allocate for this thread's stack.  <b>The effect of the
     * {@code stackSize} parameter, if any, is highly platform dependent.</b>
     * 翻译：此构造函数等同于 Thread(ThreadGroup,Runnable,String)，不同之处在于它允许指定线程堆栈大小。堆栈大小是虚拟机为此线程堆栈分配的地址空间的近似字节数。 stackSize 参数的影响（如果有）高度依赖于平台。
     *
     * <p>On some platforms, specifying a higher value for the
     * {@code stackSize} parameter may allow a thread to achieve greater
     * recursion depth before throwing a {@link StackOverflowError}.
     * Similarly, specifying a lower value may allow a greater number of
     * threads to exist concurrently without throwing an {@link
     * OutOfMemoryError} (or other internal error).  The details of
     * the relationship between the value of the <tt>stackSize</tt> parameter
     * and the maximum recursion depth and concurrency level are
     * platform-dependent.  <b>On some platforms, the value of the
     * {@code stackSize} parameter may have no effect whatsoever.</b>
     * 翻译：在某些平台上，stackSize 参数的值指定的较高可能允许线程在抛出 StackOverflowError 之前达到更大的递归深度。类似地，指定较低的值可能允许更多线程同时存在而不会引发 OutOfMemoryError（或其他内部错误）。 stackSize 参数的值与最大递归深度和并发级别之间的关系细节取决于依赖的平台。在某些平台上，stackSize 参数的值可能没有任何影响。
     *
     * <p>The virtual machine is free to treat the {@code stackSize}
     * parameter as a suggestion.  If the specified value is unreasonably low
     * for the platform, the virtual machine may instead use some
     * platform-specific minimum value; if the specified value is unreasonably
     * high, the virtual machine may instead use some platform-specific
     * maximum.  Likewise, the virtual machine is free to round the specified
     * value up or down as it sees fit (or to ignore it completely).
     * 翻译：虚拟机可以自由地将 stackSize 参数视为建议。如果在平台上指定的值过低，虚拟机可能会使用某些特定平台的最小值来代替；如果指定的值过高，虚拟机可能会使用某些特定平台的最大值来代替。同样，虚拟机可以随意向上或向下舍入它认为合适的指定值（或完全忽略它）。
     *
     * <p>Specifying a value of zero for the {@code stackSize} parameter will
     * cause this constructor to behave exactly like the
     * {@code Thread(ThreadGroup, Runnable, String)} constructor.
     * 翻译：将 stackSize 参数指定为零值将导致此构造函数的行为与 Thread(ThreadGroup, Runnable, String) 构造函数完全相同。
     *
     * <p><i>Due to the platform-dependent nature of the behavior of this
     * constructor, extreme care should be exercised in its use.
     * The thread stack size necessary to perform a given computation will
     * likely vary from one JRE implementation to another.  In light of this
     * variation, careful tuning of the stack size parameter may be required,
     * and the tuning may need to be repeated for each JRE implementation on
     * which an application is to run.</i>
     * 翻译：由于此构造函数的行为具有平台相关性，因此在使用时应格外小心。执行给定计算所需的线程堆栈大小可能因 JRE 实现而异。鉴于这种变化，可能需要仔细调整堆栈大小参数，并且可能需要针对运行应用程序的每个 JRE 实现重复调整。
     *
     * <p>Implementation note: Java platform implementers are encouraged to
     * document their implementation's behavior with respect to the
     * {@code stackSize} parameter.
     * 翻译：实现 note：鼓励 Java 平台实现者记录他们关于 stackSize 参数的实现行为。
     *
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *         线程组。如果为 null 且有安全管理器，则 group 由 SecurityManager#getThreadGroup 确定。如果没有安全管理器或 SecurityManager.getThreadGroup() 返回 null，则 group 设置为当前线程（创建新线程的线程）的线程组。
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *         启动当前线程时调用其 run 方法的对象。如果为 null，则调用当前线程的 run 方法。
     *
     * @param  name
     *         the name of the new thread
     *
     * @param  stackSize
     *         the desired stack size for the new thread, or zero to indicate
     *         that this parameter is to be ignored.
     *         新线程所需的堆栈大小，或为零表示将忽略此参数。
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     *          如果当前线程无法在指定的线程组中创建线程，则抛出 SecurityException
     *
     * @since 1.4
     */
    public Thread(ThreadGroup group, Runnable target, String name,
                  long stackSize) {
        init(group, target, name, stackSize);
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the <code>run</code> method of this thread.
     * 翻译：使该线程开始执行；JVM 调用该线程的 run 方法。
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     * 翻译：结果是两个线程并发运行：当前线程（调用 start 方法返回的线程）和另一个线程（执行其 run 方法的线程）。
     * <p>
     * It is never legal to start a thread more than once.
     * In particular, a thread may not be restarted once it has completed
     * execution.
     * 翻译：多次启动一个线程是不合法的。特别是，线程一旦完成执行就可能不会重新启动。
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     *               如果线程已经启动，则抛出 IllegalThreadStateException
     * @see        #run()
     * @see        #stop()
     */
    public synchronized void start() {
        /**
         * This method is not invoked for the main method thread or "system"
         * group threads created/set up by the VM. Any new functionality added
         * to this method in the future may have to also be added to the VM.
         * 翻译：对于由 VM 创建/设置 的主方法线程或 “系统” 组线程，不会调用此方法。将来添加到此方法的任何新功能也可能必须添加到 VM 中。
         *
         * A zero status value corresponds to state "NEW".
         * 翻译：零状态值对应状态 "NEW"。
         */
        // 启动的时候如果不是 NEW 状态，抛出 IllegalThreadStateException
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

        /* Notify the group that this thread is about to be started
         * so that it can be added to the group's list of threads
         * and the group's unstarted count can be decremented.
         * 通知 group 此线程即将启动，以便将其添加到 group 的线程列表（list of threads）中，并递减 group 中的未启动线程数。
         */
        group.add(this);

        // 当前线程是否启动成功
        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    // 通知 group 线程当前线程尝试启动失败
                    group.threadStartFailed(this);
                }
            // 用 Throwable 可以捕捉一些 Exception 捕捉不到的异常，比如说子线程抛出的异常
            } catch (Throwable ignore) {
                /* do nothing. If start0 threw a Throwable then
                  it will be passed up the call stack
                  do nothing. 如果 start0 抛出一个 Throwable 那么它将被传递到调用堆栈
                   */
            }
        }
    }

    private native void start0();

    /**
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     * 翻译：如果当前线程是使用一个分离的 Runnable 运行对象构建的，则调用该 Runnable 对象的 run 方法；否则，此方法不执行任何操作并返回。
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     * 翻译：Thread 的子类应该重写该方法。
     *
     * @see     #start()
     * @see     #stop()
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     * 翻译：该方法由系统调用，以在线程实际退出之前给它一个清理的机会。
     */
    private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        /* 积极地清空所有引用字段: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        /* 加快其中一些资源的释放 */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }

    /**
     * Forces the thread to stop executing.
     * <p>
     * If there is a security manager installed, its <code>checkAccess</code>
     * method is called with <code>this</code>
     * as its argument. This may result in a
     * <code>SecurityException</code> being raised (in the current thread).
     * <p>
     * If this thread is different from the current thread (that is, the current
     * thread is trying to stop a thread other than itself), the
     * security manager's <code>checkPermission</code> method (with a
     * <code>RuntimePermission("stopThread")</code> argument) is called in
     * addition.
     * Again, this may result in throwing a
     * <code>SecurityException</code> (in the current thread).
     * <p>
     * The thread represented by this thread is forced to stop whatever
     * it is doing abnormally and to throw a newly created
     * <code>ThreadDeath</code> object as an exception.
     * <p>
     * It is permitted to stop a thread that has not yet been started.
     * If the thread is eventually started, it immediately terminates.
     * <p>
     * An application should not normally try to catch
     * <code>ThreadDeath</code> unless it must do some extraordinary
     * cleanup operation (note that the throwing of
     * <code>ThreadDeath</code> causes <code>finally</code> clauses of
     * <code>try</code> statements to be executed before the thread
     * officially dies).  If a <code>catch</code> clause catches a
     * <code>ThreadDeath</code> object, it is important to rethrow the
     * object so that the thread actually dies.
     * <p>
     * The top-level error handler that reacts to otherwise uncaught
     * exceptions does not print out a message or otherwise notify the
     * application if the uncaught exception is an instance of
     * <code>ThreadDeath</code>.
     *
     * @exception  SecurityException  if the current thread cannot
     *               modify this thread.
     * @see        #interrupt()
     * @see        #checkAccess()
     * @see        #run()
     * @see        #start()
     * @see        ThreadDeath
     * @see        ThreadGroup#uncaughtException(Thread,Throwable)
     * @see        SecurityManager#checkAccess(Thread)
     * @see        SecurityManager#checkPermission
     * @deprecated This method is inherently unsafe.  Stopping a thread with
     *       Thread.stop causes it to unlock all of the monitors that it
     *       has locked (as a natural consequence of the unchecked
     *       <code>ThreadDeath</code> exception propagating up the stack).  If
     *       any of the objects previously protected by these monitors were in
     *       an inconsistent state, the damaged objects become visible to
     *       other threads, potentially resulting in arbitrary behavior.  Many
     *       uses of <code>stop</code> should be replaced by code that simply
     *       modifies some variable to indicate that the target thread should
     *       stop running.  The target thread should check this variable
     *       regularly, and return from its run method in an orderly fashion
     *       if the variable indicates that it is to stop running.  If the
     *       target thread waits for long periods (on a condition variable,
     *       for example), the <code>interrupt</code> method should be used to
     *       interrupt the wait.
     *       For more information, see
     *       <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *       are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     *
     *       https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     */
    @Deprecated
    public final void stop() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            checkAccess();
            if (this != Thread.currentThread()) {
                security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
            }
        }
        // A zero status value corresponds to "NEW", it can't change to
        // not-NEW because we hold the lock.
        if (threadStatus != 0) {
            resume(); // Wake up thread if it was suspended; no-op otherwise
        }

        // The VM can handle all thread states
        stop0(new ThreadDeath());
    }

    /**
     * Throws {@code UnsupportedOperationException}.
     *
     * @param obj ignored
     *
     * @deprecated This method was originally designed to force a thread to stop
     *        and throw a given {@code Throwable} as an exception. It was
     *        inherently unsafe (see {@link #stop()} for details), and furthermore
     *        could be used to generate exceptions that the target thread was
     *        not prepared to handle.
     *        For more information, see
     *        <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *        are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     *
     *        https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     */
    @Deprecated
    public final synchronized void stop(Throwable obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Interrupts this thread.
     * 翻译：中断当前线程
     *
     * <p> Unless the current thread is interrupting itself, which is
     * always permitted, the {@link #checkAccess() checkAccess} method
     * of this thread is invoked, which may cause a {@link
     * SecurityException} to be thrown.
     * 翻译：除非当前线程自己中断（这总是被允许的），否则会调用该线程的 checkAccess() 方法，这可能会导致抛出 SecurityException。
     *
     * <p> If this thread is blocked in an invocation of the {@link
     * Object#wait() wait()}, {@link Object#wait(long) wait(long)}, or {@link
     * Object#wait(long, int) wait(long, int)} methods of the {@link Object}
     * class, or of the {@link #join()}, {@link #join(long)}, {@link
     * #join(long, int)}, {@link #sleep(long)}, or {@link #sleep(long, int)},
     * methods of this class, then its interrupt status will be cleared and it
     * will receive an {@link InterruptedException}.
     * 翻译：如果此线程在调用 Object 类的 Object#wait(long)、Object#wait(long) 或 Object#wait(long, int) 方法时被阻塞 或 此类的 join()、join(long)、join(long, int)、sleep(long) 或 sleep(long, int) 方法，那么它的中断状态将被清除，并会收到一个 InterruptedException。
     *
     * <p> If this thread is blocked in an I/O operation upon an {@link
     * java.nio.channels.InterruptibleChannel InterruptibleChannel}
     * then the channel will be closed, the thread's interrupt
     * status will be set, and the thread will receive a {@link
     * java.nio.channels.ClosedByInterruptException}.
     * 翻译：如果此线程在 java.nio.channels.InterruptibleChannel 上的 IO 操作中被阻塞，则 channel 将被关闭，线程的中断状态将被设置，线程将收到一个 java.nio.channels.ClosedByInterruptException。
     *
     * <p> If this thread is blocked in a {@link java.nio.channels.Selector}
     * then the thread's interrupt status will be set and it will return
     * immediately from the selection operation, possibly with a non-zero
     * value, just as if the selector's {@link
     * java.nio.channels.Selector#wakeup wakeup} method were invoked.
     * 翻译：如果此线程在 java.nio.channels.Selector 中被阻塞，则该线程的中断状态将被设置，并且它将立即从选择操作中返回，可能具有非零值，就像调用了 selector 的 java.nio.channels.Selector#wakeup 方法一样。
     *
     * <p> If none of the previous conditions hold then this thread's interrupt
     * status will be set. </p>
     * 翻译：如果前面的条件都不成立，则将设置此线程的中断状态
     *
     * <p> Interrupting a thread that is not alive need not have any effect.
     * 翻译：中断一个不活动的线程没有任何影响。
     *
     * @throws  SecurityException
     *          if the current thread cannot modify this thread
     *          如果当前线程不能修改这个线程，则抛出 SecurityException
     *
     * @revised 6.0
     * @spec JSR-51
     */
    public void interrupt() {
        if (this != Thread.currentThread())
            checkAccess();

        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();           // Just to set the interrupt flag 只是设置中断标志
                b.interrupt(this);
                return;
            }
        }
        interrupt0();
    }

    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     * 翻译：测试当前线程是否被中断。该方法会清除线程的 中断状态。换句话说，如果这个方法被连续调用两次，第二次调用将返回 false（除非当前线程在第一次调用清除其中断状态之后和第二次调用检查它之前再次被中断）。
     *
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     * 翻译：由于线程在中断时未处于活动状态而忽略中断 将通过此方法返回 false 来反映。
     *
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     *          如果当前线程已被中断，则返回 true；否则返回 false
     * @see #isInterrupted()
     * @revised 6.0
     */
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    /**
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     * 翻译：测试此线程是否已被中断。线程的 中断状态 不受此方法影响。
     *
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     * 翻译：由于线程在中断时未处于活动状态而忽略中断 将通过此方法返回 false 来反映。
     *
     * @return  <code>true</code> if this thread has been interrupted;
     *          <code>false</code> otherwise.
     *          如果此线程已被中断，则返回 true；否则返回 false
     * @see     #interrupted()
     * @revised 6.0
     */
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * Tests if some Thread has been interrupted.  The interrupted state
     * is reset or not based on the value of ClearInterrupted that is
     * passed.
     * 测试某个线程是否已被中断。根据传递的 ClearInterrupted 值来决定是否重置中断状态。
     */
    private native boolean isInterrupted(boolean ClearInterrupted);

    /**
     * Throws {@link NoSuchMethodError}.
     *
     * @deprecated This method was originally designed to destroy this
     *     thread without any cleanup. Any monitors it held would have
     *     remained locked. However, the method was never implemented.
     *     If if were to be implemented, it would be deadlock-prone in
     *     much the manner of {@link #suspend}. If the target thread held
     *     a lock protecting a critical system resource when it was
     *     destroyed, no thread could ever access this resource again.
     *     If another thread ever attempted to lock this resource, deadlock
     *     would result. Such deadlocks typically manifest themselves as
     *     "frozen" processes. For more information, see
     *     <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">
     *     Why are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     *
     *     https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     *
     * @throws NoSuchMethodError always
     */
    @Deprecated
    public void destroy() {
        throw new NoSuchMethodError();
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     * 翻译：测试此线程是否处于活动状态。如果线程已启动且尚未死亡，则该线程处于活动状态。
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     *          如果这个线程处于活动状态，则返回 true；否则返回 false
     */
    public final native boolean isAlive();

    /**
     * Suspends this thread.
     * <p>
     * First, the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException </code>(in the current thread).
     * <p>
     * If the thread is alive, it is suspended and makes no further
     * progress unless and until it is resumed.
     *
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread.
     *               如果当前线程不能修改这个线程，则抛出 SecurityException
     * @see #checkAccess
     * @deprecated   This method has been deprecated, as it is
     *   inherently deadlock-prone.  If the target thread holds a lock on the
     *   monitor protecting a critical system resource when it is suspended, no
     *   thread can access this resource until the target thread is resumed. If
     *   the thread that would resume the target thread attempts to lock this
     *   monitor prior to calling <code>resume</code>, deadlock results.  Such
     *   deadlocks typically manifest themselves as "frozen" processes.
     *   For more information, see
     *   <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *   are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     *
     *   https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     */
    @Deprecated
    public final void suspend() {
        checkAccess();
        suspend0();
    }

    /**
     * Resumes a suspended thread.
     * <p>
     * First, the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code> (in the current thread).
     * <p>
     * If the thread is alive but suspended, it is resumed and is
     * permitted to make progress in its execution.
     *
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread.
     *               如果当前线程不能修改这个线程，则抛出 SecurityException
     * @see        #checkAccess
     * @see        #suspend()
     * @deprecated This method exists solely for use with {@link #suspend},
     *     which has been deprecated because it is deadlock-prone.
     *     For more information, see
     *     <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *     are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     *
     *     https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     */
    @Deprecated
    public final void resume() {
        checkAccess();
        resume0();
    }

    /**
     * Changes the priority of this thread.
     * 翻译：更改此线程的优先级。
     * <p>
     * First the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code>.
     * 翻译：首先，不带参数调用此线程的 checkAccess 方法。这可能会导致抛出 SecurityException。
     * <p>
     * Otherwise, the priority of this thread is set to the smaller of
     * the specified <code>newPriority</code> and the maximum permitted
     * priority of the thread's thread group.
     * 翻译：否则，该线程的优先级被设置为 指定的 newPriority 和线程的线程组的最大允许优先级 中的较小者。
     *
     * @param newPriority priority to set this thread to 为此线程设置的优先级
     * @exception  IllegalArgumentException  If the priority is not in the
     *               range <code>MIN_PRIORITY</code> to
     *               <code>MAX_PRIORITY</code>.
     *               如果优先级不在 MIN_PRIORITY 到 MAX_PRIORITY 范围内，则抛出 IllegalArgumentException
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread.
     *               如果当前线程不能修改这个线程，则抛出 SecurityException
     * @see        #getPriority
     * @see        #checkAccess()
     * @see        #getThreadGroup()
     * @see        #MAX_PRIORITY
     * @see        #MIN_PRIORITY
     * @see        ThreadGroup#getMaxPriority()
     */
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        checkAccess();
        // 优先级必须在 MIN_PRIORITY 和 MAX_PRIORITY 范围内
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        if((g = getThreadGroup()) != null) {
            // 设置为 newPriority 和线程组最大允许优先级 中的较小者
            if (newPriority > g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            // native 方法
            setPriority0(priority = newPriority);
        }
    }

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's priority.
     * @see     #setPriority
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Changes the name of this thread to be equal to the argument
     * <code>name</code>.
     * <p>
     * First the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code>.
     *
     * @param      name   the new name for this thread.
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread.
     *               如果当前线程不能修改这个线程，则抛出 SecurityException
     * @see        #getName
     * @see        #checkAccess()
     */
    public final synchronized void setName(String name) {
        checkAccess();
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;
        if (threadStatus != 0) {
            setNativeName(name);
        }
    }

    /**
     * Returns this thread's name.
     *
     * @return  this thread's name.
     * @see     #setName(String)
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the thread group to which this thread belongs.
     * This method returns null if this thread has died
     * (been stopped).
     *
     * @return  this thread's thread group.
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * Returns an estimate of the number of active threads in the current
     * thread's {@linkplain java.lang.ThreadGroup thread group} and its
     * subgroups. Recursively iterates over all subgroups in the current
     * thread's thread group.
     *
     * <p> The value returned is only an estimate because the number of
     * threads may change dynamically while this method traverses internal
     * data structures, and might be affected by the presence of certain
     * system threads. This method is intended primarily for debugging
     * and monitoring purposes.
     *
     * @return  an estimate of the number of active threads in the current
     *          thread's thread group and in any other thread group that
     *          has the current thread's thread group as an ancestor
     */
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * Copies into the specified array every active thread in the current
     * thread's thread group and its subgroups. This method simply
     * invokes the {@link java.lang.ThreadGroup#enumerate(Thread[])}
     * method of the current thread's thread group.
     *
     * <p> An application might use the {@linkplain #activeCount activeCount}
     * method to get an estimate of how big the array should be, however
     * <i>if the array is too short to hold all the threads, the extra threads
     * are silently ignored.</i>  If it is critical to obtain every active
     * thread in the current thread's thread group and its subgroups, the
     * invoker should verify that the returned int value is strictly less
     * than the length of {@code tarray}.
     *
     * <p> Due to the inherent race condition in this method, it is recommended
     * that the method only be used for debugging and monitoring purposes.
     *
     * @param  tarray
     *         an array into which to put the list of threads
     *
     * @return  the number of threads put into the array
     *
     * @throws  SecurityException
     *          if {@link java.lang.ThreadGroup#checkAccess} determines that
     *          the current thread cannot access its thread group
     */
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    /**
     * Counts the number of stack frames in this thread. The thread must
     * be suspended.
     * 翻译：计算此线程中的堆栈帧数。线程必须被挂起。
     *
     * @return     the number of stack frames in this thread.
     * @exception  IllegalThreadStateException  if this thread is not
     *             suspended.
     *             如果此线程未被挂起，则抛出 IllegalThreadStateException
     * @deprecated The definition of this call depends on {@link #suspend},
     *             which is deprecated.  Further, the results of this call
     *             were never well-defined.
     *             此调用的定义取决于已弃用的 suspend。此外，此调用的结果从未明确定义。
     */
    @Deprecated
    public native int countStackFrames();

    /**
     * Waits at most {@code millis} milliseconds for this thread to
     * die. A timeout of {@code 0} means to wait forever.
     * 翻译：最多等待 millis 毫秒以让该线程结束。0 意味着永远等待。
     *
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     * 翻译：此实现使用循环，循环中以 this.isAlive 为条件调用 this.wait。当线程终止时，调用 this.notifyAll 方法。建议应用程序不要在 Thread 实例上使用 wait、notify 或 notifyAll。
     *
     * @param  millis
     *         the time to wait in milliseconds
     *         等待时间，单位为毫秒
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
     *          如果 millis 值为负，则抛出 IllegalArgumentException
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     *          如果任何线程中断了当前线程。抛出 InterruptedException 异常时，当前线程的 中断状态 被清除。
     */
    public final synchronized void join(long millis)
    throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            // 永远等待
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                // 最多等待 millis 毫秒
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * Waits at most {@code millis} milliseconds plus
     * {@code nanos} nanoseconds for this thread to die.
     * 翻译：最多等待 millis 毫秒加上 nanos 纳秒以使该线程结束。
     *
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     * 翻译：此实现使用循环，循环中以 this.isAlive 为条件调用 this.wait。当线程终止时，调用 this.notifyAll 方法。建议应用程序不要在 Thread 实例上使用 wait、notify 或 notifyAll。
     *
     * @param  millis
     *         the time to wait in milliseconds
     *         等待时间，单位为毫秒
     *
     * @param  nanos
     *         {@code 0-999999} additional nanoseconds to wait
     *         0-999999 额外的纳秒等待时间
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative, or the value
     *          of {@code nanos} is not in the range {@code 0-999999}
     *          如果 millis 的值为负，或 nanos 的值不在 0-999999 范围内，则抛出 IllegalArgumentException
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     *          如果任何线程中断了当前线程。抛出 InterruptedException 异常时，当前线程的 中断状态 被清除。
     */
    public final synchronized void join(long millis, int nanos)
    throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        // nanos 的范围为 0-999999 纳秒
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        /**
         * 1s = 1,000ms = 1,000,000μs(微秒) = 1,000,000,000ns = 1,000,000,000,000ps(皮秒)
         * 1ms = 1,000 000ns
         * 500000ns = 0.5ms
         *
         * 有意思在于实现和注释不太相符。。
         * 注释中说，休眠时间 = 毫秒数 + 纳秒数，但其实真实情况是：
         * (1) 当 nanos 小于 0.5ms时，millis 不变；
         * (2) 当 nanos 大于等于 0.5ms时，millis 加 1；
         * (3) 当 nanos 不为 0 且 millis 为 0 时，millis 加 1
         *
         * 也就是说该方法并没有实现 ns 级别的等待，最小的等待时间为 1ms
         *
         * java.lang.Thread#sleep(long, int) 中也有这么一段
         */
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }

    /**
     * Waits for this thread to die.
     * 翻译：等待此线程结束
     *
     * <p> An invocation of this method behaves in exactly the same
     * way as the invocation
     * 翻译：此方法的调用行为与调用方式完全相同
     *
     * <blockquote>
     * {@linkplain #join(long) join}{@code (0)}
     * </blockquote>
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     *          如果任何线程中断了当前线程。抛出 InterruptedException 异常时，当前线程的 中断状态 被清除。
     */
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * Prints a stack trace of the current thread to the standard error stream.
     * This method is used only for debugging.
     *
     * @see     Throwable#printStackTrace()
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * Marks this thread as either a {@linkplain #isDaemon daemon} thread
     * or a user thread. The Java Virtual Machine exits when the only
     * threads running are all daemon threads.
     * 翻译：标记此线程为守护线程或用户线程。当运行的线程都是守护线程时，Java 虚拟机可以退出。
     *
     * <p> This method must be invoked before the thread is started.
     * 翻译：必须在线程启动之前调用此方法。
     *
     * @param  on
     *         if {@code true}, marks this thread as a daemon thread
     *         如果为 true，则将此线程被标记为守护线程
     *
     * @throws  IllegalThreadStateException
     *          if this thread is {@linkplain #isAlive alive}
     *          如果这个线程处于活动状态，则抛出 IllegalThreadStateException
     *
     * @throws  SecurityException
     *          if {@link #checkAccess} determines that the current
     *          thread cannot modify this thread
     *          如果 checkAccess 确定当前线程不能修改这个线程，则抛出 SecurityException
     */
    public final void setDaemon(boolean on) {
        checkAccess();
        // 必须在线程启动之前调用
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        daemon = on;
    }

    /**
     * Tests if this thread is a daemon thread.
     * 翻译：测试此线程是否为守护线程。
     *
     * @return  <code>true</code> if this thread is a daemon thread;
     *          <code>false</code> otherwise.
     * @see     #setDaemon(boolean)
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * Determines if the currently running thread has permission to
     * modify this thread.
     * <p>
     * If there is a security manager, its <code>checkAccess</code> method
     * is called with this thread as its argument. This may result in
     * throwing a <code>SecurityException</code>.
     *
     * @exception  SecurityException  if the current thread is not allowed to
     *               access this thread.
     * @see        SecurityManager#checkAccess(Thread)
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    /**
     * Returns a string representation of this thread, including the
     * thread's name, priority, and thread group.
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," +
                           group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + "," +
                            "" + "]";
        }
    }

    /**
     * Returns the context ClassLoader for this Thread. The context
     * ClassLoader is provided by the creator of the thread for use
     * by code running in this thread when loading classes and resources.
     * If not {@linkplain #setContextClassLoader set}, the default is the
     * ClassLoader context of the parent Thread. The context ClassLoader of the
     * primordial thread is typically set to the class loader used to load the
     * application.
     *
     * <p>If a security manager is present, and the invoker's class loader is not
     * {@code null} and is not the same as or an ancestor of the context class
     * loader, then this method invokes the security manager's {@link
     * SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * method with a {@link RuntimePermission RuntimePermission}{@code
     * ("getClassLoader")} permission to verify that retrieval of the context
     * class loader is permitted.
     *
     * @return  the context ClassLoader for this Thread, or {@code null}
     *          indicating the system class loader (or, failing that, the
     *          bootstrap class loader)
     *
     * @throws  SecurityException
     *          if the current thread cannot get the context ClassLoader
     *
     * @since 1.2
     */
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        if (contextClassLoader == null)
            return null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader.checkClassLoaderPermission(contextClassLoader,
                                                   Reflection.getCallerClass());
        }
        return contextClassLoader;
    }

    /**
     * Sets the context ClassLoader for this Thread. The context
     * ClassLoader can be set when a thread is created, and allows
     * the creator of the thread to provide the appropriate class loader,
     * through {@code getContextClassLoader}, to code running in the thread
     * when loading classes and resources.
     *
     * <p>If a security manager is present, its {@link
     * SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * method is invoked with a {@link RuntimePermission RuntimePermission}{@code
     * ("setContextClassLoader")} permission to see if setting the context
     * ClassLoader is permitted.
     *
     * @param  cl
     *         the context ClassLoader for this Thread, or null  indicating the
     *         system class loader (or, failing that, the bootstrap class loader)
     *
     * @throws  SecurityException
     *          if the current thread cannot set the context ClassLoader
     *
     * @since 1.2
     */
    public void setContextClassLoader(ClassLoader cl) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
        contextClassLoader = cl;
    }

    /**
     * Returns <tt>true</tt> if and only if the current thread holds the
     * monitor lock on the specified object.
     *
     * <p>This method is designed to allow a program to assert that
     * the current thread already holds a specified lock:
     * <pre>
     *     assert Thread.holdsLock(obj);
     * </pre>
     *
     * @param  obj the object on which to test lock ownership
     * @throws NullPointerException if obj is <tt>null</tt>
     * @return <tt>true</tt> if the current thread holds the monitor lock on
     *         the specified object.
     * @since 1.4
     */
    public static native boolean holdsLock(Object obj);

    private static final StackTraceElement[] EMPTY_STACK_TRACE
        = new StackTraceElement[0];

    /**
     * Returns an array of stack trace elements representing the stack dump
     * of this thread.  This method will return a zero-length array if
     * this thread has not started, has started but has not yet been
     * scheduled to run by the system, or has terminated.
     * If the returned array is of non-zero length then the first element of
     * the array represents the top of the stack, which is the most recent
     * method invocation in the sequence.  The last element of the array
     * represents the bottom of the stack, which is the least recent method
     * invocation in the sequence.
     *
     * <p>If there is a security manager, and this thread is not
     * the current thread, then the security manager's
     * <tt>checkPermission</tt> method is called with a
     * <tt>RuntimePermission("getStackTrace")</tt> permission
     * to see if it's ok to get the stack trace.
     *
     * <p>Some virtual machines may, under some circumstances, omit one
     * or more stack frames from the stack trace.  In the extreme case,
     * a virtual machine that has no stack trace information concerning
     * this thread is permitted to return a zero-length array from this
     * method.
     *
     * @return an array of <tt>StackTraceElement</tt>,
     * each represents one stack frame.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <tt>checkPermission</tt> method doesn't allow
     *        getting the stack trace of thread.
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     *
     * @since 1.5
     */
    public StackTraceElement[] getStackTrace() {
        if (this != Thread.currentThread()) {
            // check for getStackTrace permission
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(
                    SecurityConstants.GET_STACK_TRACE_PERMISSION);
            }
            // optimization so we do not call into the vm for threads that
            // have not yet started or have terminated
            if (!isAlive()) {
                return EMPTY_STACK_TRACE;
            }
            StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
            StackTraceElement[] stackTrace = stackTraceArray[0];
            // a thread that was alive during the previous isAlive call may have
            // since terminated, therefore not having a stacktrace.
            if (stackTrace == null) {
                stackTrace = EMPTY_STACK_TRACE;
            }
            return stackTrace;
        } else {
            // Don't need JVM help for current thread
            return (new Exception()).getStackTrace();
        }
    }

    /**
     * Returns a map of stack traces for all live threads.
     * The map keys are threads and each map value is an array of
     * <tt>StackTraceElement</tt> that represents the stack dump
     * of the corresponding <tt>Thread</tt>.
     * The returned stack traces are in the format specified for
     * the {@link #getStackTrace getStackTrace} method.
     *
     * <p>The threads may be executing while this method is called.
     * The stack trace of each thread only represents a snapshot and
     * each stack trace may be obtained at different time.  A zero-length
     * array will be returned in the map value if the virtual machine has
     * no stack trace information about a thread.
     *
     * <p>If there is a security manager, then the security manager's
     * <tt>checkPermission</tt> method is called with a
     * <tt>RuntimePermission("getStackTrace")</tt> permission as well as
     * <tt>RuntimePermission("modifyThreadGroup")</tt> permission
     * to see if it is ok to get the stack trace of all threads.
     *
     * @return a <tt>Map</tt> from <tt>Thread</tt> to an array of
     * <tt>StackTraceElement</tt> that represents the stack trace of
     * the corresponding thread.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <tt>checkPermission</tt> method doesn't allow
     *        getting the stack trace of thread.
     * @see #getStackTrace
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     *
     * @since 1.5
     */
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // check for getStackTrace permission
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                SecurityConstants.GET_STACK_TRACE_PERMISSION);
            security.checkPermission(
                SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        }

        // Get a snapshot of the list of all threads
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for (int i = 0; i < threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if (stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // else terminated so we don't put it in the map
        }
        return m;
    }


    private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
                    new RuntimePermission("enableContextClassLoaderOverride");

    /** cache of subclass security audit results */
    /* Replace with ConcurrentReferenceHashMap when/if it appears in a future
     * release */
    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits =
            new ConcurrentHashMap<>();

        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue =
            new ReferenceQueue<>();
    }

    /**
     * Verifies that this (possibly subclass) instance can be constructed
     * without violating security constraints: the subclass must not override
     * security-sensitive non-final methods, or else the
     * "enableContextClassLoaderOverride" RuntimePermission is checked.
     * 翻译：验证可以在不违反安全约束的情况下构造此（可能是子类）实例：子类不得重写安全敏感的非 final 方法，否则检查 “enableContextClassLoaderOverride” RuntimePermission。
     */
    private static boolean isCCLOverridden(Class<?> cl) {
        if (cl == Thread.class)
            return false;

        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if (result == null) {
            result = Boolean.valueOf(auditSubclass(cl));
            Caches.subclassAudits.putIfAbsent(key, result);
        }

        return result.booleanValue();
    }

    /**
     * Performs reflective checks on given subclass to verify that it doesn't
     * override security-sensitive non-final methods.  Returns true if the
     * subclass overrides any of the methods, false otherwise.
     * 翻译：对给定的子类执行反射检查以验证它没有重写安全敏感的非 final 方法。如果子类重写了任何方法，则返回 true，否则返回 false。
     */
    private static boolean auditSubclass(final Class<?> subcl) {
        Boolean result = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    for (Class<?> cl = subcl;
                         cl != Thread.class;
                         cl = cl.getSuperclass())
                    {
                        try {
                            cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                        try {
                            Class<?>[] params = {ClassLoader.class};
                            cl.getDeclaredMethod("setContextClassLoader", params);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                    }
                    return Boolean.FALSE;
                }
            }
        );
        return result.booleanValue();
    }

    private native static StackTraceElement[][] dumpThreads(Thread[] threads);
    private native static Thread[] getThreads();

    /**
     * Returns the identifier of this Thread.  The thread ID is a positive
     * <tt>long</tt> number generated when this thread was created.
     * The thread ID is unique and remains unchanged during its lifetime.
     * When a thread is terminated, this thread ID may be reused.
     * 翻译：返回此线程的标识符。线程 ID 是创建此线程时生成的正 long 数字。
     * 线程 ID 是唯一的，并且在其生命周期内保持不变。当一个线程终止时，这个线程 ID 可能会被重用。
     *
     * @return this thread's ID.
     * @since 1.5
     */
    public long getId() {
        return tid;
    }

    /**
     * A thread state.  A thread can be in one of the following states:
     * 翻译：线程状态。线程可以处于以下状态之一：
     * <ul>
     * <li>{@link #NEW}<br>
     *     A thread that has not yet started is in this state.
     *     尚未启动的线程处于 NEW 状态。
     *     </li>
     * <li>{@link #RUNNABLE}<br>
     *     A thread executing in the Java virtual machine is in this state.
     *     在 JVM 中执行的线程处于 RUNNABLE 状态。
     *     </li>
     * <li>{@link #BLOCKED}<br>
     *     A thread that is blocked waiting for a monitor lock
     *     is in this state.
     *     被阻塞等待监视器锁的线程处于 BLOCKED 状态。
     *     </li>
     * <li>{@link #WAITING}<br>
     *     A thread that is waiting indefinitely for another thread to
     *     perform a particular action is in this state.
     *     无限期等待另一个线程执行特定操作的线程处于 WAITING 状态。
     *     </li>
     * <li>{@link #TIMED_WAITING}<br>
     *     A thread that is waiting for another thread to perform an action
     *     for up to a specified waiting time is in this state.
     *     等待另一个线程执行操作直到指定等待时间的线程处于 TIMED_WAITING 状态。
     *     </li>
     * <li>{@link #TERMINATED}<br>
     *     A thread that has exited is in this state.
     *     已退出的线程处于 TERMINATED 状态。
     *     </li>
     * </ul>
     *
     * <p>
     * A thread can be in only one state at a given point in time.
     * These states are virtual machine states which do not reflect
     * any operating system thread states.
     * 翻译：一个线程在给定的时间点只能处于一种状态。这些状态不反映 任何操作系统线程状态的 虚拟机状态。
     *
     * @since   1.5
     * @see #getState
     */
    public enum State {
        /**
         * Thread state for a thread which has not yet started.
         * 尚未启动的线程状态。
         */
        NEW,

        /**
         * Thread state for a runnable thread.  A thread in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         * 可运行线程的线程状态。处于可运行状态的线程正在 JVM 中执行，但它可能正在等待来自操作系统的其他资源，例如处理器。
         */
        RUNNABLE,

        /**
         * Thread state for a thread blocked waiting for a monitor lock.
         * A thread in the blocked state is waiting for a monitor lock
         * to enter a synchronized block/method or
         * reenter a synchronized block/method after calling
         * {@link Object#wait() Object.wait}.
         * 线程阻塞等待监视器锁的线程状态。处于阻塞状态的线程正在等待监视器锁进入同步 阻塞/方法 或在调用 Object#wait() 后重入同步 阻塞/方法。
         */
        BLOCKED,

        /**
         * Thread state for a waiting thread.
         * A thread is in the waiting state due to calling one of the
         * following methods:
         * 等待线程的线程状态。由于调用以下方法之一，线程处于等待状态：
         * Object#wait() / join() / LockSupport#park()
         * <ul>
         *   <li>{@link Object#wait() Object.wait} with no timeout</li>
         *   <li>{@link #join() Thread.join} with no timeout</li>
         *   <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         *
         * <p>A thread in the waiting state is waiting for another thread to
         * perform a particular action.
         * 处于等待状态的线程正在等待另一个线程执行特定操作。
         *
         * For example, a thread that has called <tt>Object.wait()</tt>
         * on an object is waiting for another thread to call
         * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on
         * that object. A thread that has called <tt>Thread.join()</tt>
         * is waiting for a specified thread to terminate.
         * 例如，一个线程在一个对象上调用了 Object.wait() 方法，正在等待另一个线程在那个对象上调用 Object.notify() 方法或 Object.notifyAll() 方法。调用 Thread.join() 的线程正在等待指定的线程终止。
         */
        WAITING,

        /**
         * Thread state for a waiting thread with a specified waiting time.
         * A thread is in the timed waiting state due to calling one of
         * the following methods with a specified positive waiting time:
         * 指定等待时间的等待线程的线程状态。由于 使用指定的正等待时间 调用以下方法之一，线程处于 timed waiting 状态：
         * sleep / Object#wait(long) / join(long) / LockSupport#parkNanos / LockSupport#parkUntil
         * <ul>
         *   <li>{@link #sleep Thread.sleep}</li>
         *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
         *   <li>{@link #join(long) Thread.join} with timeout</li>
         *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
         *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
         * </ul>
         */
        TIMED_WAITING,

        /**
         * Thread state for a terminated thread.
         * The thread has completed execution.
         * 终止线程的线程状态。线程已完成执行。
         */
        TERMINATED;
    }

    /**
     * Returns the state of this thread.
     * This method is designed for use in monitoring of the system state,
     * not for synchronization control.
     * 翻译：返回此线程的状态。
     * 此方法设计用于监控系统状态，而不是用于同步控制。
     *
     * @return this thread's state.
     * @since 1.5
     */
    public State getState() {
        // get current thread state
        return sun.misc.VM.toThreadState(threadStatus);
    }

    // Added in JSR-166

    /**
     * Interface for handlers invoked when a <tt>Thread</tt> abruptly
     * terminates due to an uncaught exception.
     * <p>When a thread is about to terminate due to an uncaught exception
     * the Java Virtual Machine will query the thread for its
     * <tt>UncaughtExceptionHandler</tt> using
     * {@link #getUncaughtExceptionHandler} and will invoke the handler's
     * <tt>uncaughtException</tt> method, passing the thread and the
     * exception as arguments.
     * If a thread has not had its <tt>UncaughtExceptionHandler</tt>
     * explicitly set, then its <tt>ThreadGroup</tt> object acts as its
     * <tt>UncaughtExceptionHandler</tt>. If the <tt>ThreadGroup</tt> object
     * has no
     * special requirements for dealing with the exception, it can forward
     * the invocation to the {@linkplain #getDefaultUncaughtExceptionHandler
     * default uncaught exception handler}.
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @see #setUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        /**
         * Method invoked when the given thread terminates due to the
         * given uncaught exception.
         * <p>Any exception thrown by this method will be ignored by the
         * Java Virtual Machine.
         * @param t the thread
         * @param e the exception
         */
        void uncaughtException(Thread t, Throwable e);
    }

    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * Set the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception, and no other handler has been defined
     * for that thread.
     *
     * <p>Uncaught exception handling is controlled first by the thread, then
     * by the thread's {@link ThreadGroup} object and finally by the default
     * uncaught exception handler. If the thread does not have an explicit
     * uncaught exception handler set, and the thread's thread group
     * (including parent thread groups)  does not specialize its
     * <tt>uncaughtException</tt> method, then the default handler's
     * <tt>uncaughtException</tt> method will be invoked.
     * <p>By setting the default uncaught exception handler, an application
     * can change the way in which uncaught exceptions are handled (such as
     * logging to a specific device, or file) for those threads that would
     * already accept whatever &quot;default&quot; behavior the system
     * provided.
     *
     * <p>Note that the default uncaught exception handler should not usually
     * defer to the thread's <tt>ThreadGroup</tt> object, as that could cause
     * infinite recursion.
     *
     * @param eh the object to use as the default uncaught exception handler.
     * If <tt>null</tt> then there is no default handler.
     *
     * @throws SecurityException if a security manager is present and it
     *         denies <tt>{@link RuntimePermission}
     *         (&quot;setDefaultUncaughtExceptionHandler&quot;)</tt>
     *
     * @see #setUncaughtExceptionHandler
     * @see #getUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                new RuntimePermission("setDefaultUncaughtExceptionHandler")
                    );
        }

         defaultUncaughtExceptionHandler = eh;
     }

    /**
     * Returns the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception. If the returned value is <tt>null</tt>,
     * there is no default.
     * @since 1.5
     * @see #setDefaultUncaughtExceptionHandler
     * @return the default uncaught exception handler for all threads
     */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }

    /**
     * Returns the handler invoked when this thread abruptly terminates
     * due to an uncaught exception. If this thread has not had an
     * uncaught exception handler explicitly set then this thread's
     * <tt>ThreadGroup</tt> object is returned, unless this thread
     * has terminated, in which case <tt>null</tt> is returned.
     * @since 1.5
     * @return the uncaught exception handler for this thread
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ?
            uncaughtExceptionHandler : group;
    }

    /**
     * Set the handler invoked when this thread abruptly terminates
     * due to an uncaught exception.
     * <p>A thread can take full control of how it responds to uncaught
     * exceptions by having its uncaught exception handler explicitly set.
     * If no such handler is set then the thread's <tt>ThreadGroup</tt>
     * object acts as its handler.
     * @param eh the object to use as this thread's uncaught exception
     * handler. If <tt>null</tt> then this thread has no explicit handler.
     * @throws  SecurityException  if the current thread is not allowed to
     *          modify this thread.
     * @see #setDefaultUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }

    /**
     * Dispatch an uncaught exception to the handler. This method is
     * intended to be called only by the JVM.
     */
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }

    /**
     * Removes from the specified map any keys that have been enqueued
     * on the specified reference queue.
     */
    static void processQueue(ReferenceQueue<Class<?>> queue,
                             ConcurrentMap<? extends
                             WeakReference<Class<?>>, ?> map)
    {
        Reference<? extends Class<?>> ref;
        while((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    /**
     *  Weak key for Class objects.
     **/
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        private final int hash;

        /**
         * Create a new WeakClassKey to the given object, registered
         * with a queue.
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }

        /**
         * Returns the identity hash code of the original referent.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns true if the given object is this identical
         * WeakClassKey instance, or, if this object's referent has not
         * been cleared, if the given object is another WeakClassKey
         * instance with the identical non-null referent as this one.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) &&
                       (referent == ((WeakClassKey) obj).get());
            } else {
                return false;
            }
        }
    }


    // The following three initially uninitialized fields are exclusively
    // managed by class java.util.concurrent.ThreadLocalRandom. These
    // fields are used to build the high-performance PRNGs in the
    // concurrent code, and we can not risk accidental false sharing.
    // Hence, the fields are isolated with @Contended.

    /** The current seed for a ThreadLocalRandom */
    @sun.misc.Contended("tlr")
    long threadLocalRandomSeed;

    /** Probe hash value; nonzero if threadLocalRandomSeed initialized */
    @sun.misc.Contended("tlr")
    int threadLocalRandomProbe;

    /** Secondary seed isolated from public ThreadLocalRandom sequence */
    @sun.misc.Contended("tlr")
    int threadLocalRandomSecondarySeed;

    /* Some private helper methods */
    private native void setPriority0(int newPriority);
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
    private native void setNativeName(String name);
}
