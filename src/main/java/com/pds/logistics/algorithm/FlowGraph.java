package com.pds.logistics.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjacency-list based directed graph supporting Min-Cost Max-Flow operations.
 *
 * <p>Nodes are indexed 0..n-1. Super-source = n-2, super-sink = n-1 by convention.
 * The graph stores both forward and reverse (residual) edges in the same adjacency list.
 */
public class FlowGraph {

    private final int         n;         // total number of nodes
    private final List<List<Edge>> adj;  // adjacency list

    public FlowGraph(int n) {
        this.n   = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
    }

    /**
     * Adds a directed edge u → v with given capacity and cost.
     * Automatically creates the paired reverse edge v → u with 0 capacity and -cost.
     *
     * @param u       source node
     * @param v       destination node
     * @param cap     capacity (≥ 0)
     * @param cost    unit cost (≥ 0 for forward, < 0 for reverse)
     * @param routeId DB route ID (0 = synthetic super-source/sink edge)
     * @return the forward Edge object added
     */
    public Edge addEdge(int u, int v, double cap, double cost, int routeId) {
        int forwardIndex  = adj.get(u).size();
        int backwardIndex = adj.get(v).size();

        Edge forward  = new Edge(v, cap,   cost,  backwardIndex, routeId);
        Edge backward = new Edge(u, 0,    -cost,  forwardIndex,  routeId);

        adj.get(u).add(forward);
        adj.get(v).add(backward);

        return forward;
    }

    /** Returns the adjacency list for node u */
    public List<Edge> getAdj(int u) {
        return adj.get(u);
    }

    /** Returns the reverse edge of a given edge */
    public Edge getReverse(int from, Edge e) {
        return adj.get(e.to).get(e.revIndex);
    }

    public int getNodeCount() { return n; }

    /** Returns all forward edges (cap > 0 originally, routeId > 0) */
    public List<Edge> getAllForwardEdges() {
        List<Edge> result = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            for (Edge e : adj.get(u)) {
                if (e.routeId > 0 && e.originalCap > 0) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "FlowGraph{nodes=%d}".formatted(n);
    }
}
