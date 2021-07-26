/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The <code>Runnable</code> interface should be implemented by any
 * class whose instances are intended to be executed by a thread. The
 * class must define a method of no arguments called <code>run</code>.
 * 翻译：Runnable 接口应该由 其实例打算由一个线程来执行的任何类实现。该类必须定义一个名为 run 的无参方法。
 * <p>
 * This interface is designed to provide a common protocol for objects that
 * wish to execute code while they are active. For example,
 * <code>Runnable</code> is implemented by class <code>Thread</code>.
 * Being active simply means that a thread has been started and has not
 * yet been stopped.
 * 翻译：此接口旨在为 希望在活动状态执行代码 的对象提供通用协议。例如，Thread 类实现了 Runnable。处于活动状态仅意味着线程已启动且尚未停止。
 * <p>
 * In addition, <code>Runnable</code> provides the means for a class to be
 * active while not subclassing <code>Thread</code>. A class that implements
 * <code>Runnable</code> can run without subclassing <code>Thread</code>
 * by instantiating a <code>Thread</code> instance and passing itself in
 * as the target.  In most cases, the <code>Runnable</code> interface should
 * be used if you are only planning to override the <code>run()</code>
 * method and no other <code>Thread</code> methods.
 * This is important because classes should not be subclassed
 * unless the programmer intends on modifying or enhancing the fundamental
 * behavior of the class.
 * 翻译：此外，Runnable 提供了 使类处于活动状态而不用作为 Thread 子类 的方法。通过实例化 Thread 实例并将自身作为 target 传入，实现 Runnable 的类可以在不继承 Thread 的情况下运行。在大多数情况下，如果你只计划重写 run() 方法而不是其他 Thread 方法，则应该使用 Runnable 接口。
 * 这很重要，除非程序员打算修改或增强类的基本行为，否则类不应被子类化。
 *
 * @author  Arthur van Hoff
 * @see     java.lang.Thread
 * @see     java.util.concurrent.Callable
 * @since   JDK1.0
 */
@FunctionalInterface
public interface Runnable {
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * 翻译：当使用实现 Runnable 接口的对象创建线程时，启动线程会导致在该分离的执行线程中调用对象的 run 方法。
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     * 翻译：run 方法的一般约定是它可以采取任何操作。
     *
     * @see     java.lang.Thread#run()
     */
    public abstract void run();
}
