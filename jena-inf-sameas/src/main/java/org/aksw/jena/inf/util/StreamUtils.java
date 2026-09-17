package org.aksw.jena.inf.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamUtils {
	static <T> Stream<T> breadthFirst(
	        T start,
	        Function<T, ? extends Iterable<T>> successors) {

	    var seen = new HashSet<T>();
	    var queue = new ArrayDeque<T>();
	    queue.add(start);

	    return Stream.iterate(
	            start,
	            ignored -> !queue.isEmpty(),
	            ignored -> {
	                var current = queue.remove();
	                seen.add(current);

	                for (var next : successors.apply(current)) {
	                    if (seen.add(next)) {
	                        queue.add(next);
	                    }
	                }

	                return queue.peek();
	            });
	}
}
