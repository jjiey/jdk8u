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
 * An object that creates new threads on demand.  Using thread factories
 * removes hardwiring of calls to {@link Thread#Thread(Runnable) new Thread},
 * enabling applications to use special thread subclasses, priorities, etc.
 * 翻译：按需创建新线程的对象。使用线程工厂消除了对 Thread#Thread(Runnable) new Thread 的直接调用，使应用程序能够使用特殊的线程子类、优先级等。
 *
 * <p>
 * The simplest implementation of this interface is just:
 * 翻译：此接口最简单的实现就是：
 *  <pre> {@code
 * class SimpleThreadFactory implements ThreadFactory {
 *   public Thread newThread(Runnable r) {
 *     return new Thread(r);
 *   }
 * }}</pre>
 *
 * The {@link Executors#defaultThreadFactory} method provides a more
 * useful simple implementation, that sets the created thread context
 * to known values before returning it.
 * 翻译：Executors#defaultThreadFactory 方法提供了一个更有用的简单实现，它在返回之前将创建的线程上下文设置为已知值。
 * @since 1.5
 * @author Doug Lea
 */
public interface ThreadFactory {

    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     * 翻译：构造一个新的 Thread。实现还可以初始化优先级、名称、守护线程状态、线程组等。
     *
     * @param r a runnable to be executed by new thread instance 由新线程实例执行的 runnable
     * @return constructed thread, or {@code null} if the request to
     *         create a thread is rejected
     *         返回构造的线程 或 null（如果创建线程的请求被拒绝）
     */
    Thread newThread(Runnable r);
}
