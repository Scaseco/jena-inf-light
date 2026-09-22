package org.aksw.jena.inf.sameas.assembler;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.aksw.jena.inf.sameas.DatasetGraphSameAs;
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
    @Override
    public DatasetGraph createDataset(Assembler a, Resource root) {
        Resource baseDatasetRes = root.getPropertyResourceValue(DatasetAssemblerVocab.pDataset);
        Objects.requireNonNull(baseDatasetRes, "No ja:baseDataset specified on " + root);
        Object obj = a.open(baseDatasetRes);

        boolean allowDuplicates = Optional.ofNullable(root.getProperty(SameAsVocab.allowDuplicates)).map(Statement::getBoolean).orElse(false);

        Set<Node> predicates = root.listProperties(SameAsVocab.predicate).mapWith(Statement::getResource).mapWith(Resource::asNode).toSet();
        if (predicates.isEmpty()) {
            predicates.add(OWL2.sameAs.asNode());
        }

        DatasetGraph result;
        if (obj instanceof Dataset) {
            Dataset baseDataset = (Dataset)obj;
            DatasetGraph base = baseDataset.asDatasetGraph();

            result = DatasetGraphSameAs.wrap(base, predicates, allowDuplicates);
        } else {
            Class<?> cls = obj == null ? null : obj.getClass();
            throw new AssemblerException(root, "Expected ja:baseDataset to be a Dataset but instead got " + Objects.toString(cls));
        }
        return result;
    }
}
