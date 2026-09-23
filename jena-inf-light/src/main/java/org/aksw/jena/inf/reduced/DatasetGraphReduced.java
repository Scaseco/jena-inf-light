package org.aksw.jena.inf.reduced;

import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfs.engine.DatasetGraphWithGraphTransform;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphWrapperView;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

public class DatasetGraphReduced extends DatasetGraphWithGraphTransform implements DatasetGraphWrapperView {
	// Do not unwrap for query execution.
    private final long cacheMaxSize;

    public DatasetGraphReduced(DatasetGraph dsg, long cacheMaxSize) {
        super(dsg, g -> new GraphReduced(g, cacheMaxSize));
        this.cacheMaxSize = cacheMaxSize;
    }

    public DatasetGraphReduced(DatasetGraph dsg, long cacheMaxSize, Context cxt) {
        super(dsg, cxt, g -> new GraphReduced(g, cacheMaxSize));
        this.cacheMaxSize = cacheMaxSize;
    }

    public long getCacheMaxSize() {
		return cacheMaxSize;
	}

    @Override // TODO Needs to become part of DatasetGraphWithGraphTransform
    public Stream<Quad> stream(Node g, Node s, Node p, Node o) {
    	return Iter.asStream(find(g, s, p, o));
    }

    @Override // TODO Needs to become part of DatasetGraphWithGraphTransform
    public Stream<Quad> stream() {
    	return Iter.asStream(find());
    }
}
