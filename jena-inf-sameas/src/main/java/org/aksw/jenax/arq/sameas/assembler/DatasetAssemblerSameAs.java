package org.aksw.jenax.arq.sameas.assembler;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.aksw.jenax.arq.dataset.cache.CachePatterns;
import org.aksw.jenax.arq.dataset.cache.DatasetGraphCache;
import org.aksw.jenax.arq.util.dataset.DatasetGraphSameAs;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;
import org.apache.jena.sparql.core.assembler.DatasetAssemblerVocab;
import org.apache.jena.vocabulary.OWL2;

public class DatasetAssemblerSameAs
    extends DatasetAssembler
{
//  @Iri(SameAsTerms.baseDataset)
//  Resource getBaseDataset();
//  void setBaseDataset(Resource baseDataset);
//
//  @Iri(SameAsTerms.NS + "predicate")
//  Set<Node> getPredicates();
//
//  @Iri(SameAsTerms.NS + "cacheSize")
//  Integer getCacheSize();
//  SameAsConfig setCacheSize(Integer value);
//
//  @Iri(SameAsTerms.NS + "allowDuplicates")
//  Boolean getAllowDuplicates();
//  SameAsConfig setAllowDuplicates(Boolean value);


    @Override
    public DatasetGraph createDataset(Assembler a, Resource root) {
        Resource baseDatasetRes = root.getPropertyResourceValue(DatasetAssemblerVocab.pDataset);
        Objects.requireNonNull(baseDatasetRes, "No ja:baseDataset specified on " + root);
        Object obj = a.open(baseDatasetRes);

        int cacheSizeMax = Optional.ofNullable(root.getProperty(SameAsVocab.cacheSize)).map(Statement::getInt).orElse(0);
        boolean allowDuplicates = Optional.ofNullable(root.getProperty(SameAsVocab.allowDuplicates)).map(Statement::getBoolean).orElse(false);

        Set<Node> predicates = root.listProperties(SameAsVocab.predicates).mapWith(Statement::getResource).mapWith(Resource::asNode).toSet();
        if (predicates.isEmpty()) {
            predicates.add(OWL2.sameAs.asNode());
        }

        DatasetGraph result;
        if (obj instanceof Dataset) {
            Dataset baseDataset = (Dataset)obj;
            DatasetGraph base = baseDataset.asDatasetGraph();

            // A negative value for caching loads all patterns into the cache
            // The idea is that if we know that e.g. all outgoing/incoming sameAs links are cached
            // then whenever there is a cache miss we know that there is no data and can skip the request
            // to the backing graph
            if (cacheSizeMax > 0) {
                DatasetGraph cache = DatasetGraphCache.cache(base, CachePatterns.forNeigborsByPredicates(predicates), cacheSizeMax);
                result = DatasetGraphSameAs.wrap(cache, predicates, allowDuplicates);
            } else if (cacheSizeMax < 0) {
                // base = DatasetGraphCache.table(base, CachePatterns.forNeigborsByPredicates(predicates));
                result = DatasetGraphSameAs.wrapWithTable(base, predicates, allowDuplicates);
            } else {
                result = DatasetGraphSameAs.wrap(base, predicates, allowDuplicates);
            }

           // result = DatasetGraphSameAs.wrap(base, predicates, allowDuplicates);
           // result = DatasetGraphSameAsOld.wrap(base, predicates, allowDuplicates);
        } else {
            Class<?> cls = obj == null ? null : obj.getClass();
            throw new AssemblerException(root, "Expected ja:baseDataset to be a Dataset but instead got " + Objects.toString(cls));
        }
        return result;
    }
}
