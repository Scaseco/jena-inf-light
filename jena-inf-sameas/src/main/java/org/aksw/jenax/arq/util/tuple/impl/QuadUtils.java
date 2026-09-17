package org.aksw.jenax.arq.util.tuple.impl;

import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.NodeUtils;

public class QuadUtils {
    /**
     * Create a quad from an array
     *
     * @param nodes
     * @return
     */
    public static Quad create(Node[] nodes) {
        return Quad.create(nodes[0], nodes[1], nodes[2], nodes[3]);
    }

    public static Quad createMatch(Node g, Node s, Node p, Node o) {
        return Quad.create(
                NodeUtils.nullToAny(g),
                NodeUtils.nullToAny(s),
                NodeUtils.nullToAny(p),
                NodeUtils.nullToAny(o));
    }

    /** Create o stream of a quad's four nodes */
    public static Stream<Node> streamNodes(Quad q) {
        return Stream.of(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject());
    }

    /** Access a triple's component by a zero-based index in order g, s, p, o.
     * Raises {@link IndexOutOfBoundsException} for any index outside of the range [0, 3]*/
    public static Node getNode(Quad quad, int idx) {
        switch (idx) {
        case 0: return quad.getGraph();
        case 1: return quad.getSubject();
        case 2: return quad.getPredicate();
        case 3: return quad.getObject();
        default: throw new IndexOutOfBoundsException("Cannot access index " + idx + " of a quad");
        }
    }
}
