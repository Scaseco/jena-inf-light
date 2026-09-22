package org.aksw.jena.inf.sameas.assembler;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class SameAsVocab {
    public static final Resource DatasetSameAs = ResourceFactory.createResource(SameAsTerms.DatasetSameAs);
    public static final Property predicate = ResourceFactory.createProperty(SameAsTerms.predicate);
    public static final Property cacheSize = ResourceFactory.createProperty(SameAsTerms.cacheSize);
    public static final Property allowDuplicates = ResourceFactory.createProperty(SameAsTerms.allowDuplicates);
    public static final Property allowReflexive = ResourceFactory.createProperty(SameAsTerms.allowReflexive);
}
