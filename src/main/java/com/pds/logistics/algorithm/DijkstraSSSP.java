package com.pds.logistics.algorithm;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Dijkstra's Single-Source Shortest Path with node potentials (Johnson's re-weighting).
 *
 * <p>Used internally by the MCMF solver to find minimum-cost augmenting paths in the
 * residual graph. Because residual edges can have negative costs (reverse arcs), we
 * maintain <em>node potentials</em> (h[]) from the previous iteration so that all
 * reduced-cost edges satisfy: reducedCost = cost + h[u] - h[v] ≥ 0.
 * This lets us safely use Dijkstra (which requires non-negative edge weights) at each
 * augmentation step instead of the slower Bellman-Ford, giving O(E log V) per path.
 *
 * <p>Reference: Successive Shortest Path Algorithm with Johnson potentials.
 */
public class DijkstraSSSP {

    public static final double INF = Double.MAX_VALUE / 2.0;

    /**
     * Runs Dijkstra on the residual graph from {@code source}, respecting only edges
     * with positive residual capacity.
     *
     * @param graph     the residual flow graph
     * @param source    start node
     * @param potential current node potential array h[] (length = graph.nodeCount)
     * @return ShortestPathResult with dist[], prevNode[], prevEdge[]
     */
    public static ShortestPathResult run(FlowGraph graph, int source, double[] potential) {

        int n = graph.getNodeCount();

        double[] dist     = new double[n];
        int[]    prevNode = new int[n];
        Edge[]   prevEdge = new Edge[n];

        Arrays.fill(dist,     INF);
        Arrays.fill(prevNode, -1);

        dist[source] = 0.0;

        // PriorityQueue: (reducedDist, node)
        PriorityQueue<double[]> pq = new PriorityQueue<>(
                (a, b) -> Double.compare(a[0], b[0])
        );
        pq.offer(new double[]{0.0, source});

        while (!pq.isEmpty()) {
            double[] top  = pq.poll();
            double   d    = top[0];
            int      u    = (int) top[1];

            if (d > dist[u]) continue; // stale entry

            for (Edge e : graph.getAdj(u)) {
                if (e.cap <= 1e-9) continue; // no residual capacity

                // Reduced cost using Johnson potentials: must be ≥ 0
                double h_u = (potential[u]  >= INF) ? 0 : potential[u];
                double h_v = (potential[e.to] >= INF) ? 0 : potential[e.to];
                double reducedCost = e.cost + h_u - h_v;

                // Clamp tiny negatives from floating-point drift
                if (reducedCost < 0) reducedCost = 0;

                double newDist = dist[u] + reducedCost;
                if (newDist < dist[e.to] - 1e-9) {
                    dist[e.to]     = newDist;
                    prevNode[e.to] = u;
                    prevEdge[e.to] = e;
                    pq.offer(new double[]{newDist, e.to});
                }
            }
        }

        return new ShortestPathResult(dist, prevNode, prevEdge, potential);
    }

    // ─── Inner Result Record ──────────────────────────────────────────────────

    public record ShortestPathResult(
            double[] dist,
            int[]    prevNode,
            Edge[]   prevEdge,
            double[] oldPotential
    ) {
        /** Returns true if sink is reachable from source */
        public boolean reachable(int sink) {
            return dist[sink] < INF;
        }

        /**
         * Computes the updated Johnson potential array for the next iteration.
         * newPotential[v] = oldPotential[v] + dist[v]   (INF if unreachable)
         */
        public double[] updatedPotentials() {
            int n = dist.length;
            double[] newPotential = new double[n];
            for (int v = 0; v < n; v++) {
                if (dist[v] < INF) {
                    double old = (oldPotential[v] >= INF) ? 0 : oldPotential[v];
                    newPotential[v] = old + dist[v];
                } else {
                    newPotential[v] = INF;
                }
            }
            return newPotential;
        }

        /**
         * Reconstructs the augmenting path from source to sink as an array of edges.
         * Returns null if sink is unreachable.
         */
        public Edge[] reconstructPath(int source, int sink) {
            if (!reachable(sink)) return null;

            java.util.List<Edge> path = new java.util.ArrayList<>();
            int cur = sink;
            while (cur != source) {
                path.add(prevEdge[cur]);
                cur = prevNode[cur];
            }
            java.util.Collections.reverse(path);
            return path.toArray(new Edge[0]);
        }

        /**
         * Returns the actual (non-reduced) cost of the path using original potentials.
         * Actual cost = reducedDist[sink] + h[source] - h[sink]  (cancelled out)
         * = dist[sink] + oldPotential[source] - oldPotential[sink]
         */
        public double actualCostToSink(int source, int sink) {
            if (!reachable(sink)) return INF;
            double h_s = (oldPotential[source] >= INF) ? 0 : oldPotential[source];
            double h_t = (oldPotential[sink]   >= INF) ? 0 : oldPotential[sink];
            return dist[sink] - h_s + h_t;
        }
    }
}
