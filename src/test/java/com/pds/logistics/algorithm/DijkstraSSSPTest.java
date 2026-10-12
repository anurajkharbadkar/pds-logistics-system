package com.pds.logistics.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dijkstra SSSP with Johnson Potentials Tests")
class DijkstraSSSPTest {

    @Test
    @DisplayName("Should find direct shortest path in simple 3-node graph")
    void testDirectShortestPath() {
        FlowGraph graph = new FlowGraph(3);
        // 0 -> 1 with cap 10, cost 5
        graph.addEdge(0, 1, 10.0, 5.0, 1);
        // 1 -> 2 with cap 10, cost 3
        graph.addEdge(1, 2, 10.0, 3.0, 2);
        // 0 -> 2 with cap 10, cost 12
        graph.addEdge(0, 2, 10.0, 12.0, 3);

        double[] potential = new double[]{0.0, 0.0, 0.0};
        DijkstraSSSP.ShortestPathResult result = DijkstraSSSP.run(graph, 0, potential);

        assertTrue(result.reachable(2), "Node 2 should be reachable from 0");
        assertEquals(8.0, result.dist()[2], 1e-6, "Distance 0->1->2 should be 5 + 3 = 8.0");

        Edge[] path = result.reconstructPath(0, 2);
        assertNotNull(path);
        assertEquals(2, path.length);
        assertEquals(1, path[0].to);
        assertEquals(2, path[1].to);
    }

    @Test
    @DisplayName("Should respect zero capacity and ignore saturated edges")
    void testZeroCapacityIgnored() {
        FlowGraph graph = new FlowGraph(2);
        // 0 -> 1 with 0 capacity
        graph.addEdge(0, 1, 0.0, 2.0, 1);

        double[] potential = new double[]{0.0, 0.0};
        DijkstraSSSP.ShortestPathResult result = DijkstraSSSP.run(graph, 0, potential);

        assertFalse(result.reachable(1), "Edge with 0 residual capacity must not be traversed");
    }

    @Test
    @DisplayName("Should correctly handle node potentials with non-negative reduced costs")
    void testJohnsonPotentials() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 5.0, 10.0, 1);
        graph.addEdge(1, 2, 5.0, 5.0, 2);

        // Potentials h[0]=0, h[1]=10, h[2]=15
        // Reduced cost 0->1: 10 + 0 - 10 = 0
        // Reduced cost 1->2: 5 + 10 - 15 = 0
        double[] potential = new double[]{0.0, 10.0, 15.0};
        DijkstraSSSP.ShortestPathResult result = DijkstraSSSP.run(graph, 0, potential);

        assertTrue(result.reachable(2));
        assertEquals(0.0, result.dist()[2], 1e-6, "Reduced cost under potentials should be 0.0");
        assertEquals(15.0, result.actualCostToSink(0, 2), 1e-6, "Actual unreduced path cost should be 15.0");
    }
}
