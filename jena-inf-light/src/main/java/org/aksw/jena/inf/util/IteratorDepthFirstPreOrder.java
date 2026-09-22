package org.aksw.jena.inf.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

public class IteratorDepthFirstPreOrder<T> implements Iterator<T> {

    private final Function<? super T, ? extends Iterable<? extends T>> successors;
    private final Set<T> seen = new HashSet<>();
    private final Deque<Iterator<? extends T>> stack = new ArrayDeque<>();

    private T next;

    public IteratorDepthFirstPreOrder(
            T start,
            Function<? super T, ? extends Iterable<? extends T>> successors) {
        this.successors = successors;
        this.next = start;
        seen.add(start);
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        if (next == null) {
            throw new NoSuchElementException();
        }

        T result = next;
        stack.push(successors.apply(result).iterator());
        next = advance();

        return result;
    }

    private T advance() {
        while (!stack.isEmpty()) {
            Iterator<? extends T> it = stack.peek();

            if (!it.hasNext()) {
                stack.pop();
                continue;
            }

            T candidate = it.next();
            if (seen.add(candidate)) {
                return candidate;
            }
        }

        return null;
    }
}
