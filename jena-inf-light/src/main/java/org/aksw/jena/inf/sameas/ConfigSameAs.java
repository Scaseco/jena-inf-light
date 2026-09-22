package org.aksw.jena.inf.sameas;

import java.util.Set;
import java.util.function.Predicate;

public record ConfigSameAs<C>(
	Set<C> sameAsPredicates,
	boolean allowDuplicates,
	Predicate<C> mayHaveSameAsLinks
) {}
