package org.aksw.jena.inf.reduced;

import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.rdfs.engine.Match;
import org.apache.jena.rdfs.engine.MatchWrapper;

public class MatchReduced<X, T>
	extends MatchWrapper<X, T, Match<X, T>>
{
	private long cacheMaxSize;

	public MatchReduced(Match<X, T> delegate, long cacheMaxSize) {
		super(delegate);
		if (cacheMaxSize < 0) {
			throw new IllegalArgumentException("cacheMaxSize must be >= 0. Got: " + cacheMaxSize);
		}
		this.cacheMaxSize = cacheMaxSize;
	}

	@Override
	public Stream<T> match(X s, X p, X o) {
		Stream<T> base = super.match(s, p, o);
		int clampedSize = Math.clamp(cacheMaxSize, 0, Integer.MAX_VALUE);
		Stream<T> result = (cacheMaxSize == 0)
			? base
			: (cacheMaxSize == 1)
				? Iter.asStream(Iter.distinctAdjacent(Iter.ofStream(base)))
				: Iter.asStream(Iter.distinctCached(Iter.ofStream(base), clampedSize));
		return result;
	}
}
