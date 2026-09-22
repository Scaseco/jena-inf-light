package org.aksw.jena.inf.sameas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.jena.inf.util.CacheUtils;
import org.aksw.jena.inf.util.IterUtils;
import org.aksw.jena.inf.util.SparqlCxt;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.rdfs.engine.MapperX;
import org.apache.jena.rdfs.engine.Match;
import org.apache.jena.rdfs.engine.MatchWrapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.graph.Traverser;

public class MatchSameAs<C, D>
	extends MatchWrapper<C, D, Match<C, D>>
{
    protected boolean logCacheStats = false;

    /** Allowing duplicates disables 'contains' checks for inferred triples which may increase performance */
    protected boolean allowDuplicates;

    protected SparqlCxt<C> sparqlCxt;
    protected Set<C> sameAsPredicates;

    /** If true then drop <b>inferred</b> (x sameAs x) triples. Does not drop physical triples. */
    protected boolean dropReflexive = true;

    // Optional predicate used to prune computations if if resources are known not to have any sameAs links
    protected Predicate<C> mayHaveSameAsLinks;

	public MatchSameAs(Match<C, D> delegate) {
		super(delegate);
	}

	@Override
	public Stream<D> match(C s, C p, C o) {
        return new Worker(s, p, o).find();
	}

    public static <C, D> Match<C, D> wrap(Match<C, D> base, SparqlCxt<C> sparqlCxt, C sameAsPredicate) {
        return wrap(base, sparqlCxt, Collections.singleton(sameAsPredicate), false);
    }

    public static <C, D> Match<C, D> wrap(Match<C, D> base, SparqlCxt<C> sparqlCxt, Set<C> sameAsPredicates) {
        return wrap(base, sparqlCxt, sameAsPredicates, false);
    }

    public static <C, D> Match<C, D> wrap(Match<C, D> base, SparqlCxt<C> sparqlCxt, Set<C> sameAsPredicates, boolean allowDuplicates) {
        return wrap(base, sparqlCxt, sameAsPredicates, allowDuplicates, null);
    }

    public static <C, D> Match<C, D> wrap(Match<C, D> base, SparqlCxt<C> sparqlCxt, Set<C> sameAsPredicates, boolean allowDuplicates, Predicate<C> mayHaveSameAsLinks) {
        return new MatchSameAs<>(base, sparqlCxt, sameAsPredicates, allowDuplicates, mayHaveSameAsLinks);
    }

    protected MatchSameAs(Match<C, D> base, SparqlCxt<C> sparqlCxt, Set<C> sameAsPredicates, boolean allowDuplicates, Predicate<C> mayHaveSameAsLinks) {
        super(base);
        this.sparqlCxt = sparqlCxt;
        this.sameAsPredicates = sameAsPredicates;
        this.mayHaveSameAsLinks = mayHaveSameAsLinks;
        this.allowDuplicates = allowDuplicates;
    }

    class Worker {
        protected C ms, mp, mo;

        // These caches are local to the find() call in order to avoid complex synchronization w.r.t. updates
        // Cache sizes could be made configurable.
        protected Cache<C, List<C>> sameAsCache =
                CacheUtils.recordStats(Caffeine.newBuilder(), logCacheStats)
                	.maximumSize(10).build();

        // This cache on our load did not generate sufficient hits - therefore disabled
        protected Cache<D, D> leastQuadCache = null; // CacheBuilder.newBuilder().recordStats().concurrencyLevel(1).maximumSize(100).build();

        public Worker(C ms, C mp, C mo) {
            super();
            this.ms = ms;
            this.mp = mp;
            this.mo = mo;
        }

        public Stream<D> find() {
            // If s or o is concrete then resolve their sameAs closure before the lookup
            // Otherwise, resolve the sameAs closures of that component based on the obtained triple's concrete values

            // Note: resolveSameAs only actually resolves the given start node if both it and the graph are concrete;
            // Otherwise the start node is returned as-is.

            // Set up the tuple stream (quads)
            List<C> initialSubjects = resolveSameAsSortedCached(ms);
            List<C> initialObjects = resolveSameAsSortedCached(mo);

            Iterator<D> result =
                Iter.iter(initialSubjects).flatMap(s ->
                    Iter.iter(initialObjects).flatMap(o ->
                        IterUtils.iter(getDelegate().match(s, mp, o))
                            .flatMap(t -> streamInferencesOnLeastQuad(t))
                    ));

            // Log cache stats once the iterator is closed
            if (logCacheStats) {
                result = Iter.onClose(result, () -> {
                    System.out.println("SameAsCache: "+ CacheUtils.stats(sameAsCache));
                    System.out.println("LeastQuadCache: "+ CacheUtils.stats(leastQuadCache));
                });
            }

            return Iter.asStream(result);
            // return result;
        }

        private Iterator<D> streamInferencesOnLeastQuad(D quad) {
            Iterator<D> result;
        	MapperX<C, D> mapper = getMapper();
            C p = mapper.predicate(quad);
            List<C> sortedSubjects = resolveSameAsSortedCached(mapper.subject(quad));
            List<C> sortedObjects = resolveSameAsSortedCached(mapper.object(quad));

            // The quad and the same-as closures of the subjects and objects
            // are the basis to create the set of inferred quads
            // However, inferences should only be emitted on the 'least' quad that is contained in the graph
            // For performance it is crucial to minimize lookups via contains()

            if (sortedSubjects.size() == 1 && sortedObjects.size() == 1) {
                // Don't call contains if there is there are no same-as'd alternatives of it
                result = Iter.of(quad);
            } else {
                C leastS = sortedSubjects.get(0);
                C leastO = sortedObjects.get(0);
                D leastInferrableQuad = mapper.tuple(leastS, p, leastO);
                D leastPhysicalQuad;

                boolean isLeastQuad;
                if (!allowDuplicates) {
                    leastPhysicalQuad = CacheUtils.get(leastQuadCache, leastInferrableQuad, () -> computeLeastPhysicalQuad(leastInferrableQuad, sortedSubjects, sortedObjects));
                    isLeastQuad = quad.equals(leastPhysicalQuad);
                } else {
                    isLeastQuad = true;
                }
                // If there were concrete subjects/objects for matching then restrict the cross-join
                // only to those
                List<C> ss = sparqlCxt.isConcrete(ms) ? Collections.singletonList(ms) : sortedSubjects;
                List<C> oo = sparqlCxt.isConcrete(mo) ? Collections.singletonList(mo) : sortedObjects;

                // System.out.println(quad + (isLeastQuad ? " is least quad " : " hasLeastQuad: " + leastQuad));
                result = isLeastQuad
                        ? Iter.iter(ss).flatMap(s -> Iter.iter(oo)
                                .filter(o -> !(dropReflexive && sameAsPredicates.contains(p) && Objects.equals(s, o)))
                                .map(o -> mapper.tuple(s, p, o)))
                        : Iter.empty();

    //            if (!isLeastQuad) {
    //                System.out.println("IsLeast: " + isLeastQuad + " " + quad);
    //            }
            }
            return result;
        }

        private List<C> resolveSameAsSortedCached(C start) {
            List<C> result;
            if (!sparqlCxt.isConcrete(start) || sparqlCxt.isLiteral(start)) {
                result = Collections.singletonList(start);
            } else {
                // Entry<C, C> startKey = Map.entry(g, start);
            	C startKey = start;

                result = CacheUtils.getIfPresent(sameAsCache, startKey);
                if (result == null) {
                    if (mayHaveSameAsLinks != null && !mayHaveSameAsLinks.test(start)) {
                        // Shortcut which does not write to cache
                        result = Collections.singletonList(start);
                    }
                }

                if (result == null) {
                    boolean[] wasComputed = { false };
                    result = CacheUtils.get(sameAsCache, startKey, () -> {
                        wasComputed[0] = true;
                        return resolveSameAsSorted(start);
                    });

                    // From experiments it is cheaper to compute the closure for resources
                    // only when needed - rather than spending time on preemptively adding cache entries
                    boolean updateClosureForAllMembers = false;
                    if (updateClosureForAllMembers) {
                        // Add cache entries for all nodes in the closure
                        if (wasComputed[0]) {
                            for (C node : result) {
                                if (!node.equals(start)) {
                                    sameAsCache.put(node, result);
                                }
                            }
                            // Ensure that the startKey is marked as recently used
                            sameAsCache.put(startKey, result);
                        }
                    }
                }
            }
            return result;
        }
    }

    private D computeLeastPhysicalQuad(D quad, List<C> sortedSubjects, List<C> sortedObjects) {
        // Note: This method can unexpectedly return null (leading to an NPE)
        // if the backing dataset's find() method returns quads for which contains() returns false
    	MapperX<C, D> mapper = getMapper();
        D result = null;
        C p = mapper.predicate(quad);
        // Note: Even if either the subjects or objects array has a size of 1
        // we need to perform the contains check to determine which inferrable quad is in the graph

        outer: for (C s : sortedSubjects) {
            for (C o : sortedObjects) {
                // System.out.println("Lookup: " + s + " - " + o);

                // Note: We can either create the cross product between subjects and objects
                // Or we e.g. lookup the os for a subjects and find the least one within the objects
                // We could add a threshold to the cross product size as when to resort to graph lookups instead.
                if (getDelegate().contains(s, p, o)) {
                    result = mapper.tuple(s, p, o);
                    break outer;
                }
            }
        }

        if (result == null) {
            throw new IllegalStateException("Tuple [" + quad + "] was unexpectedly reported to not be contained in the backend");
        }

        return result;
    }

    /**
     * Collect the same as closure into a sorted list (non-cached).
     * Always returns an ArrayList. to e.g.
     */
    private List<C> resolveSameAsSorted(C start) {
        List<C> result = resolveSameAs(start).collect(Collectors.toCollection(ArrayList::new));
        Collections.sort(result, sparqlCxt.comparator());
        return result;
    }

    /**
     *
     * @param g
     * @param start
     * @return A stream of unique start's same-as reachable nodes.
     */
    private Iter<C> resolveSameAs(C start) {
        Traverser<C> traverser = Traverser.forGraph(n -> getDirectNodes(n));
        // Note: Traverser always includes the start node in its result
        Iter<C> result = Iter.iter(traverser.depthFirstPreOrder(start).iterator());
        // result = StreamUtils.viaList(result, list -> System.out.println("resolveSameAs [greaterOrEqual=" + greaterOrEqual + "]: " + start + " -> " + list));
        return result;
    }

    private Set<C> getDirectNodes(C start) {
        Set<C> result;
        result = sparqlCxt.isLiteral(start)
            ? Collections.emptySet()
            : loadDirectNodes(start);
        return result;
    }

    /** This method should only be invoked by the cache callback */
    private Set<C> loadDirectNodes(C s) {
        Set<C> result = findDirectTriples(s).toSet();
        return result;
    }

    /** Stream direct triples in both directions */
    private Iter<C> findDirectTriples(C s) {
        return Iter.concat(
            Iter.iter(sameAsPredicates).flatMap(p -> findDirectNodes(s, p, true)),
            Iter.iter(sameAsPredicates).flatMap(p -> findDirectNodes(s, p, false)));
    }

    /** Stream direct triples in a specific direction (ingoing or outgoing) */
    private Iter<C> findDirectNodes(C s, C p, boolean isForward) {
    	MapperX<C, D> mapper = getMapper();
        Iter<C> result = isForward
                ? IterUtils.iter(getDelegate().match(s, p, sparqlCxt.any())).map(mapper::object)
                : IterUtils.iter(getDelegate().match(sparqlCxt.any(), p, s)).map(mapper::subject);
        return result;
    }
}
