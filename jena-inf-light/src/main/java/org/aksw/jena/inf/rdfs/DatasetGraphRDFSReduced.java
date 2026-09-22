package org.aksw.jena.inf.rdfs;

import org.aksw.jena.inf.reduced.DatasetGraphReduced;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdfs.DatasetGraphRDFS;
import org.apache.jena.rdfs.RDFSFactory;
import org.apache.jena.rdfs.SetupRDFS;
import org.apache.jena.sparql.core.DatasetGraph;

// Not a real DatasetGraph implementation but could be extended to one.
// It's not clear whether the reduced feature could be integrated closer into Jena's existing RDFS inferencer.
public class DatasetGraphRDFSReduced {
	public static DatasetGraph wrap(DatasetGraph base, Graph vocab) {
		SetupRDFS setup = RDFSFactory.setupRDFS(vocab);
		return wrap(base, setup, 50_000);
	}

	public static DatasetGraph wrap(DatasetGraph base, SetupRDFS setup) {
		return wrap(base, setup, 50_000);
	}

	public static DatasetGraph wrap(DatasetGraph base, SetupRDFS setup, long cacheMaxSize) {
        DatasetGraph rdfsBase = new DatasetGraphRDFS(base, setup);
        DatasetGraph dsg = new DatasetGraphReduced(rdfsBase, cacheMaxSize);
        return dsg;
	}
}
