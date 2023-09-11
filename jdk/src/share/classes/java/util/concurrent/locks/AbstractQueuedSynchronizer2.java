//package java.util.concurrent.locks;
//public abstract class AbstractQueuedSynchronizer2
//    extends AbstractOwnableSynchronizer
//    implements java.io.Serializable {
//
//    /**
//     * Returns {@code true} if synchronization is held exclusively with
//     * respect to the current (calling) thread.  This method is invoked
//     * upon each call to a non-waiting {@link ConditionObject} method.
//     * (Waiting methods instead invoke {@link #release}.)
//     * 如果同步针对当前（调用）线程是独占的，则返回 true。每次调用非等待 {@link ConditionObject} 方法时都会调用此方法。（等待方法改为调用 release。）
//     *
//     * <p>The default implementation throws {@link
//     * UnsupportedOperationException}. This method is invoked
//     * internally only within {@link ConditionObject} methods, so need
//     * not be defined if conditions are not used.
//     * 翻译：默认实现抛出 UnsupportedOperationException。此方法仅在 ConditionObject 方法内部调用，因此如果不使用 condition 则无需定义。
//     *
//     * @return {@code true} if synchronization is held exclusively;
//     *         {@code false} otherwise
//     * @throws UnsupportedOperationException if conditions are not supported
//     */
//    protected boolean isHeldExclusively() {
//        throw new UnsupportedOperationException();
//    }
//
//    // Queue inspection methods
//    // 队列检查方法
//
//    /**
//     * Queries whether any threads are waiting to acquire. Note that
//     * because cancellations due to interrupts and timeouts may occur
//     * at any time, a {@code true} return does not guarantee that any
//     * other thread will ever acquire.
//     *
//     * <p>In this implementation, this operation returns in
//     * constant time.
//     *
//     * @return {@code true} if there may be other threads waiting to acquire
//     */
//    public final boolean hasQueuedThreads() {
//        return head != tail;
//    }
//
//    /**
//     * Queries whether any threads have ever contended to acquire this
//     * synchronizer; that is if an acquire method has ever blocked.
//     *
//     * <p>In this implementation, this operation returns in
//     * constant time.
//     *
//     * @return {@code true} if there has ever been contention
//     */
//    public final boolean hasContended() {
//        return head != null;
//    }
//
//    /**
//     * Returns the first (longest-waiting) thread in the queue, or
//     * {@code null} if no threads are currently queued.
//     *
//     * <p>In this implementation, this operation normally returns in
//     * constant time, but may iterate upon contention if other threads are
//     * concurrently modifying the queue.
//     *
//     * @return the first (longest-waiting) thread in the queue, or
//     *         {@code null} if no threads are currently queued
//     */
//    public final Thread getFirstQueuedThread() {
//        // handle only fast path, else relay
//        return (head == tail) ? null : fullGetFirstQueuedThread();
//    }
//
//    /**
//     * Version of getFirstQueuedThread called when fastpath fails
//     */
//    private Thread fullGetFirstQueuedThread() {
//        /*
//         * The first node is normally head.next. Try to get its
//         * thread field, ensuring consistent reads: If thread
//         * field is nulled out or s.prev is no longer head, then
//         * some other thread(s) concurrently performed setHead in
//         * between some of our reads. We try this twice before
//         * resorting to traversal.
//         */
//        Node h, s;
//        Thread st;
//        if (((h = head) != null && (s = h.next) != null &&
//             s.prev == head && (st = s.thread) != null) ||
//            ((h = head) != null && (s = h.next) != null &&
//             s.prev == head && (st = s.thread) != null))
//            return st;
//
//        /*
//         * Head's next field might not have been set yet, or may have
//         * been unset after setHead. So we must check to see if tail
//         * is actually first node. If not, we continue on, safely
//         * traversing from tail back to head to find first,
//         * guaranteeing termination.
//         */
//
//        Node t = tail;
//        Thread firstThread = null;
//        while (t != null && t != head) {
//            Thread tt = t.thread;
//            if (tt != null)
//                firstThread = tt;
//            t = t.prev;
//        }
//        return firstThread;
//    }
//
//    /**
//     * Returns true if the given thread is currently queued.
//     *
//     * <p>This implementation traverses the queue to determine
//     * presence of the given thread.
//     *
//     * @param thread the thread
//     * @return {@code true} if the given thread is on the queue
//     * @throws NullPointerException if the thread is null
//     */
//    public final boolean isQueued(Thread thread) {
//        if (thread == null)
//            throw new NullPointerException();
//        for (Node p = tail; p != null; p = p.prev)
//            if (p.thread == thread)
//                return true;
//        return false;
//    }
//
//    /**
//     * Returns {@code true} if the apparent first queued thread, if one
//     * exists, is waiting in exclusive mode.  If this method returns
//     * {@code true}, and the current thread is attempting to acquire in
//     * shared mode (that is, this method is invoked from {@link
//     * #tryAcquireShared}) then it is guaranteed that the current thread
//     * is not the first queued thread.  Used only as a heuristic in
//     * ReentrantReadWriteLock.
//     */
//    final boolean apparentlyFirstQueuedIsExclusive() {
//        Node h, s;
//        return (h = head) != null &&
//            (s = h.next)  != null &&
//            !s.isShared()         &&
//            s.thread != null;
//    }
//
//    /**
//     * Queries whether any threads have been waiting to acquire longer
//     * than the current thread.
//     * 翻译：查询是否有任何线程等待获取的时间 比当前线程长。
//     *
//     * <p>An invocation of this method is equivalent to (but may be
//     * more efficient than):
//     * 翻译：调用此方法等效于（但可能比 下面的代码 更有效）：
//     *  <pre> {@code
//     * getFirstQueuedThread() != Thread.currentThread() &&
//     * hasQueuedThreads()}</pre>
//     *
//     * <p>Note that because cancellations due to interrupts and
//     * timeouts may occur at any time, a {@code true} return does not
//     * guarantee that some other thread will acquire before the current
//     * thread.  Likewise, it is possible for another thread to win a
//     * race to enqueue after this method has returned {@code false},
//     * due to the queue being empty.
//     * 翻译：注意，由于中断和超时导致的取消随时可能发生，因此返回 true 并不能保证其他线程会在当前线程之前获取。同样，由于队列为空，在此方法返回 false 后，另一个线程可能会赢得入队竞争。
//     *
//     * <p>This method is designed to be used by a fair synchronizer to
//     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
//     * Such a synchronizer's {@link #tryAcquire} method should return
//     * {@code false}, and its {@link #tryAcquireShared} method should
//     * return a negative value, if this method returns {@code true}
//     * (unless this is a reentrant acquire).  For example, the {@code
//     * tryAcquire} method for a fair, reentrant, exclusive mode
//     * synchronizer might look like this:
//     * 翻译：此方法旨在供公平同步器使用，以避免 插入（barging）。这样一个同步器的 #tryAcquire 方法应该返回 false，并且它的 #tryAcquireShared 方法应该返回一个负值，如果此方法返回 true（除非这是一个可重入获取）。例如，公平、可重入、独占模式同步器的 #tryAcquire 方法可能如下所示：
//     *
//     *  <pre> {@code
//     * protected boolean tryAcquire(int arg) {
//     *   if (isHeldExclusively()) {
//     *     // A reentrant acquire; increment hold count
//     *     // 可重入获取；增加保留计数
//     *     return true;
//     *   } else if (hasQueuedPredecessors()) {
//     *     return false;
//     *   } else {
//     *     // try to acquire normally
//     *     // 尝试正常获取
//     *   }
//     * }}</pre>
//     *
//     * @return {@code true} if there is a queued thread preceding the
//     *         current thread, and {@code false} if the current thread
//     *         is at the head of the queue or the queue is empty
//     *         如果当前线程前面有一个排队线程，则返回 true；如果当前线程在队列的头部或队列为空，则返回 false
//     * @since 1.7
//     */
//    public final boolean hasQueuedPredecessors() {
//        // The correctness of this depends on head being initialized
//        // before tail and on head.next being accurate if the current
//        // thread is first in queue.
//        // 此操作的正确性取决于 head 在 tail 之前被初始化，以及，如果当前线程是队列中的第一个，head.next 是准确的。
//        Node t = tail; // Read fields in reverse initialization order 以相反的初始化顺序读取字段
//        Node h = head;
//        Node s;
//        return h != t &&
//            ((s = h.next) == null || s.thread != Thread.currentThread());
//    }
//
//
//    // Instrumentation and monitoring methods
//
//    /**
//     * Returns an estimate of the number of threads waiting to
//     * acquire.  The value is only an estimate because the number of
//     * threads may change dynamically while this method traverses
//     * internal data structures.  This method is designed for use in
//     * monitoring system state, not for synchronization
//     * control.
//     *
//     * @return the estimated number of threads waiting to acquire
//     */
//    public final int getQueueLength() {
//        int n = 0;
//        for (Node p = tail; p != null; p = p.prev) {
//            if (p.thread != null)
//                ++n;
//        }
//        return n;
//    }
//
//    /**
//     * Returns a collection containing threads that may be waiting to
//     * acquire.  Because the actual set of threads may change
//     * dynamically while constructing this result, the returned
//     * collection is only a best-effort estimate.  The elements of the
//     * returned collection are in no particular order.  This method is
//     * designed to facilitate construction of subclasses that provide
//     * more extensive monitoring facilities.
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<Thread>();
//        for (Node p = tail; p != null; p = p.prev) {
//            Thread t = p.thread;
//            if (t != null)
//                list.add(t);
//        }
//        return list;
//    }
//
//    /**
//     * Returns a collection containing threads that may be waiting to
//     * acquire in exclusive mode. This has the same properties
//     * as {@link #getQueuedThreads} except that it only returns
//     * those threads waiting due to an exclusive acquire.
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getExclusiveQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<Thread>();
//        for (Node p = tail; p != null; p = p.prev) {
//            if (!p.isShared()) {
//                Thread t = p.thread;
//                if (t != null)
//                    list.add(t);
//            }
//        }
//        return list;
//    }
//
//    /**
//     * Returns a collection containing threads that may be waiting to
//     * acquire in shared mode. This has the same properties
//     * as {@link #getQueuedThreads} except that it only returns
//     * those threads waiting due to a shared acquire.
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getSharedQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<Thread>();
//        for (Node p = tail; p != null; p = p.prev) {
//            if (p.isShared()) {
//                Thread t = p.thread;
//                if (t != null)
//                    list.add(t);
//            }
//        }
//        return list;
//    }
//
//    /**
//     * Returns a string identifying this synchronizer, as well as its state.
//     * The state, in brackets, includes the String {@code "State ="}
//     * followed by the current value of {@link #getState}, and either
//     * {@code "nonempty"} or {@code "empty"} depending on whether the
//     * queue is empty.
//     *
//     * @return a string identifying this synchronizer, as well as its state
//     */
//    public String toString() {
//        int s = getState();
//        String q  = hasQueuedThreads() ? "non" : "";
//        return super.toString() +
//            "[State = " + s + ", " + q + "empty queue]";
//    }
//
//
//    // Internal support methods for Conditions
//
//    /**
//     * Returns true if a node, always one that was initially placed on
//     * a condition queue, is now waiting to reacquire on sync queue.
//     * 翻译：如果一个节点（始终是最初放置在 condition queue 中的节点）现在正在 sync queue 中等待重新获取，则返回 true。
//     * @param node the node
//     * @return true if is reacquiring 如果重新获取，则返回 true
//     */
//    final boolean isOnSyncQueue(Node node) {
//        if (node.waitStatus == Node.CONDITION || node.prev == null)
//            return false;
//        if (node.next != null) // If has successor, it must be on queue 如果有后继，它肯定在队列中
//            return true;
//        /*
//         * node.prev can be non-null, but not yet on queue because
//         * the CAS to place it on queue can fail. So we have to
//         * traverse from tail to make sure it actually made it.  It
//         * will always be near the tail in calls to this method, and
//         * unless the CAS failed (which is unlikely), it will be
//         * there, so we hardly ever traverse much.
//         * 翻译：node.prev 可以不为 null 但尚未在队列中，因为将其放入队列的 CAS 操作可能会失败。所以我们必须从尾部遍历以确保它确实成功了。在调用这个方法时它总是靠近 tail，除非 CAS 失败（这不太可能），它会在那里，所以我们几乎不会遍历太多。
//         */
//        return findNodeFromTail(node);
//    }
//
//    /**
//     * Returns true if node is on sync queue by searching backwards from tail.
//     * Called only when needed by isOnSyncQueue.
//     * 翻译：如果节点通过从 tail 向前搜索在 sync queue 中，则返回 true。仅在 isOnSyncQueue 需要时调用。
//     * @return true if present
//     *
//     * 只有 isOnSyncQueue() 方法调用
//     */
//    private boolean findNodeFromTail(Node node) {
//        Node t = tail;
//        for (;;) {
//            if (t == node)
//                return true;
//            if (t == null)
//                return false;
//            t = t.prev;
//        }
//    }
//
//    /**
//     * Transfers a node from a condition queue onto sync queue.
//     * Returns true if successful.
//     * 翻译：将节点从 condition queue 转移到 sync 队列。成功返回 true。
//     * @param node the node
//     * @return true if successfully transferred (else the node was
//     * cancelled before signal)
//     * todo 如果成功转移（否则节点在信号之前被取消）
//     */
//    final boolean transferForSignal(Node node) {
//        /*
//         * If cannot change waitStatus, the node has been cancelled.
//         * 如果无法更改 waitStatus，则该节点已被取消。
//         */
//        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
//            return false;
//
//        /*
//         * Splice onto queue and try to set waitStatus of predecessor to
//         * indicate that thread is (probably) waiting. If cancelled or
//         * attempt to set waitStatus fails, wake up to resync (in which
//         * case the waitStatus can be transiently and harmlessly wrong).
//         * 拼接到队列并尝试设置前驱的 waitStatus 以表明线程（可能）正在等待。如果已取消或尝试设置 waitStatus 失败，则唤醒以重新同步（在这种情况下，waitStatus 可能是 短暂（transiently） 且无害的错误）。
//         */
//        Node p = enq(node);
//        int ws = p.waitStatus;
//        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
//            LockSupport.unpark(node.thread);
//        return true;
//    }
//
//    /**
//     * Transfers node, if necessary, to sync queue after a cancelled wait.
//     * Returns true if thread was cancelled before being signalled.
//     *
//     * @param node the node
//     * @return true if cancelled before the node was signalled
//     */
//    final boolean transferAfterCancelledWait(Node node) {
//        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
//            enq(node);
//            return true;
//        }
//        /*
//         * If we lost out to a signal(), then we can't proceed
//         * until it finishes its enq().  Cancelling during an
//         * incomplete transfer is both rare and transient, so just
//         * spin.
//         */
//        while (!isOnSyncQueue(node))
//            Thread.yield();
//        return false;
//    }
//
//    /**
//     * Invokes release with current state value; returns saved state.
//     * Cancels node and throws exception on failure.
//     * 翻译：使用当前 state 值（记为 savedState）调用 release 方法；返回 savedState。失败时取消节点并抛出异常。
//     * @param node the condition node for this wait 此等待的 condition 节点
//     * @return previous sync state 之前的同步状态
//     */
//    final int fullyRelease(Node node) {
//        boolean failed = true;
//        try {
//            int savedState = getState();
//            if (release(savedState)) {
//                failed = false;
//                return savedState;
//            } else {
//                throw new IllegalMonitorStateException();
//            }
//        } finally {
//            if (failed)
//                node.waitStatus = Node.CANCELLED;
//        }
//    }
//}
