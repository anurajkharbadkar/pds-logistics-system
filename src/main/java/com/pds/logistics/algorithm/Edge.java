package com.pds.logistics.algorithm;

/**
 * Represents a directed edge in the residual flow graph.
 *
 * <p>Each real edge u→v generates two Edge objects:
 * <ul>
 *   <li>Forward edge:  cap > 0, cost = unitCost</li>
 *   <li>Backward edge: cap = 0, cost = -unitCost  (residual / cancellation)</li>
 * </ul>
 *
 * The pair of forward and backward edges are always stored consecutively in the
 * adjacency list so that edge.rev = adjacency[to][edge.revIndex].
 */
public class Edge {

    /** Destination node index in the graph */
    public final int to;

    /** Capacity (residual) remaining on this edge */
    public double cap;

    /** Unit cost per unit of flow on this edge (negative for reverse edges) */
    public final double cost;

    /** Index of this edge's reverse counterpart in graph.adj[to] */
    public final int revIndex;

    /** DB route ID (0 for super-source/sink synthetic edges) */
    public final int routeId;

    /** Original capacity (for result extraction) */
    public final double originalCap;

    public Edge(int to, double cap, double cost, int revIndex, int routeId) {
        this.to          = to;
        this.cap         = cap;
        this.cost        = cost;
        this.revIndex    = revIndex;
        this.routeId     = routeId;
        this.originalCap = cap;
    }

    public boolean isForward() {
        return cost >= 0;
    }

    /** Flow carried on this edge = originalCap - residualCap (for forward edges only) */
    public double getFlow() {
        return originalCap - cap;
    }

    @Override
    public String toString() {
        return "Edge{to=%d, cap=%.2f, cost=%.2f, routeId=%d}".formatted(to, cap, cost, routeId);
    }
}
