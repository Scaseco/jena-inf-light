package org.aksw.jena.inf.sameas;

import org.aksw.jena.inf.util.SparqlCxtNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdfs.engine.GraphMatch;
import org.apache.jena.rdfs.engine.Match;
import org.apache.jena.rdfs.engine.MatchGraph;

public class GraphSameAs
	extends GraphMatch
{
	private final ConfigSameAs<Node> config;

	public GraphSameAs(Graph graph, ConfigSameAs<Node> setupSameAs) {
		super(graph, createMatch(graph, setupSameAs));
		this.config = setupSameAs;
	}

	public ConfigSameAs<Node> getConfig() {
		return config;
	}

    private static Match<Node, Triple> createMatch(Graph graph, ConfigSameAs<Node> setupSameAs) {
        Match<Node, Triple> match = new MatchSameAs<>(new MatchGraph(graph), SparqlCxtNode.get(),
            	setupSameAs.sameAsPredicates(), setupSameAs.allowDuplicates(), setupSameAs.mayHaveSameAsLinks());
        return match;
    }
}
