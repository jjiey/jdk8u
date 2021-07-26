package yjtest;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author yangsanity
 *
 * https://en.wikipedia.org/wiki/Treiber_stack
 *
 * com.netflix.hystrix.Hystrix.ConcurrentStack 里也有
 */
public class TreiberStackTest {

    public static void main(String[] args) {

    }

    /**
     * ConcurrentStack
     *
     * Nonblocking stack using Treiber's algorithm
     *
     * @author Brian Goetz and Tim Peierls
     *
     * @ThreadSafe
     */
    static class ConcurrentStack<E> {

        AtomicReference<Node<E>> top = new AtomicReference<>();

        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            Node<E> oldHead;
            do {
                oldHead = top.get();
                newHead.next = oldHead;
            } while (!top.compareAndSet(oldHead, newHead));
        }

        public E pop() {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = top.get();
                if (oldHead == null) {
                    return null;
                }
                newHead = oldHead.next;
            } while (!top.compareAndSet(oldHead, newHead));
            return oldHead.item;
        }

        private static class Node<E> {
            public final E item;
            public Node<E> next;

            public Node(E item) {
                this.item = item;
            }
        }
    }
}
