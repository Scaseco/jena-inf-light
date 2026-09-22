package org.aksw.jena.inf.uniondefaultgraph;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class UnionDefaultGraphVocab {
    public static final String NS = "http://jenax.aksw.org/plugin#";

    public static String getURI() { return NS; }

    public static final Resource DatasetUnionDefaultGraph = ResourceFactory.createResource(NS + "DatasetUnionDefaultGraph");
    public static final Resource DatasetAutoUnionDefaultGraph = ResourceFactory.createResource(NS + "DatasetAutoUnionDefaultGraph");
}
