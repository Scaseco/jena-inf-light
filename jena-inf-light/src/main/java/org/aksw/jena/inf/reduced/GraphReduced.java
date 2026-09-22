package org.aksw.jena.inf.reduced;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdfs.engine.GraphMatch;
import org.apache.jena.rdfs.engine.Match;
import org.apache.jena.rdfs.engine.MatchGraph;

public class GraphReduced
	extends GraphMatch
{
	private final long cacheMaxSize;

	public GraphReduced(Graph graph, long cacheMaxSize) {
		super(graph, createMatch(graph, cacheMaxSize));
		this.cacheMaxSize = cacheMaxSize;
	}

	public long getCacheMaxSize() {
		return cacheMaxSize;
	}

	private static Match<Node, Triple> createMatch(Graph graph, long cacheMaxSize) {
	    Match<Node, Triple> match = new MatchReduced<>(new MatchGraph(graph), cacheMaxSize);
	    return match;
	}
}
