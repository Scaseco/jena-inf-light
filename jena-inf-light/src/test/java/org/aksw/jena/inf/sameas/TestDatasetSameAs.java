package org.aksw.jena.inf.sameas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.aksw.jena.inf.rdfs.assembler.DatasetAssemblerRdfsReduced;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.exec.QueryExec;
import org.junit.jupiter.api.Test;

public class TestDatasetSameAs {
	@Test
	public void test01() {
		DatasetGraph baseDsg = DatasetGraphFactory.create();
		RDFDataMgr.read(baseDsg, "mona-lisa.ttl");
		RDFDataMgr.read(baseDsg, "same-as.ttl");
		// 9 base triples

		Model rdfs = RDFDataMgr.loadModel("ontology.rdfs.ttl");
		DatasetAssemblerRdfsReduced.setRdfs(baseDsg.getContext(), rdfs);

		DatasetGraph sameAsDsg = DatasetGraphSameAs.wrap(baseDsg);

		{
			Table table = QueryExec.dataset(sameAsDsg).query("SELECT * { ?s ?p ?o }").table();
			// RowSetOps.out(System.out, table.toRowSet());
			assertEquals(17, table.size());
		}

		{
			Table table = QueryExec.dataset(baseDsg).query("SELECT * { SERVICE <sameAs:> { ?s ?p ?o } }").table();
			// RowSetOps.out(System.out, table.toRowSet());
			assertEquals(17, table.size());
		}

		{
			// 3 extra inferences based on foaf:topic_interest:
			// Bob a Agent .
			// dbr:Mona_Lisa a Thing .
			// wd:Q12418 a Thing .      # via owl:sameA
			Table table = QueryExec.dataset(baseDsg).query("SELECT * { SERVICE <sameAs+rdfs:> { ?s ?p ?o } }").table();
			// RowSetOps.out(System.out, table.toRowSet());
			assertEquals(20, table.size());
		}
	}
}
