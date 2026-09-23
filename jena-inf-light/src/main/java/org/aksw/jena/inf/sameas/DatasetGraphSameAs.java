package org.aksw.jena.inf.sameas;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfs.engine.DatasetGraphWithGraphTransform;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphWrapperView;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.OWL;

public class DatasetGraphSameAs extends DatasetGraphWithGraphTransform implements DatasetGraphWrapperView {

	// Do not unwrap for query execution.
    private final ConfigSameAs<Node> config;

    public DatasetGraphSameAs(DatasetGraph dsg, ConfigSameAs<Node> config) {
        super(dsg, g -> new GraphSameAs(g, config));
        this.config = config;
    }

    public ConfigSameAs<Node> getConfig() {
		return config;
	}

    @Override // TODO Needs to become part of DatasetGraphWithGraphTransform
    public Stream<Quad> stream(Node g, Node s, Node p, Node o) {
    	return Iter.asStream(find(g, s, p, o));
    }

    @Override // TODO Needs to become part of DatasetGraphWithGraphTransform
    public Stream<Quad> stream() {
    	return Iter.asStream(find());
    }

    public DatasetGraphSameAs(DatasetGraph dsg, ConfigSameAs<Node> config, Context cxt) {
        super(dsg, cxt, g -> new GraphSameAs(g, config));
        this.config = config;
    }

    public static DatasetGraph wrap(DatasetGraph base) {
        return wrap(base, OWL.sameAs.asNode());
    }

    public static DatasetGraph wrap(DatasetGraph base, Node sameAsPredicate) {
        return wrap(base, Collections.singleton(sameAsPredicate), false);
    }

    public static DatasetGraph wrap(DatasetGraph base, Set<Node> sameAsPredicates) {
        return wrap(base, sameAsPredicates, false);
    }

    public static DatasetGraph wrap(DatasetGraph base, Set<Node> sameAsPredicates, boolean allowDuplicates) {
    	ConfigSameAs<Node> config = new ConfigSameAs<>(sameAsPredicates, allowDuplicates, null);
    	return new DatasetGraphSameAs(base, config);
    }
}
