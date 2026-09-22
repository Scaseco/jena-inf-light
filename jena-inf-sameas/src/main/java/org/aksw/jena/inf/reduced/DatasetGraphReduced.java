package org.aksw.jena.inf.reduced;

import org.aksw.jena.inf.sameas.ConfigSameAs;
import org.aksw.jena.inf.sameas.GraphSameAs;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfs.engine.DatasetGraphWithGraphTransform;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphWrapperView;
import org.apache.jena.sparql.util.Context;

public class DatasetGraphReduced extends DatasetGraphWithGraphTransform implements DatasetGraphWrapperView {
	// Do not unwrap for query execution.
    private final long cacheMaxSize;

    public DatasetGraphReduced(DatasetGraph dsg, long cacheMaxSize) {
        super(dsg, g -> new GraphReduced(g, cacheMaxSize));
        this.cacheMaxSize = cacheMaxSize;
    }

    public DatasetGraphReduced(DatasetGraph dsg, long cacheMaxSize, ConfigSameAs<Node> setup, Context cxt) {
        super(dsg, cxt, g -> new GraphSameAs(g, setup));
        this.cacheMaxSize = cacheMaxSize;
    }

    public long getCacheMaxSize() {
		return cacheMaxSize;
	}
}
