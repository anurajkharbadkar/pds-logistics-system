package com.pds.logistics.algorithm;

import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Min-Cost Max-Flow (MCMF) Solver using the Successive Shortest Path (SSP) algorithm.
 *
 * <h2>Algorithm Overview</h2>
 * <ol>
 *   <li>Build a residual flow graph:
 *     <ul>
 *       <li>Add a <em>super-source</em> S connected to every supply depot with
 *           capacity = depot supply, cost = 0.</li>
 *       <li>Add a <em>super-sink</em> T connected from every demand node with
 *           capacity = demand requirement, cost = 0.</li>
 *       <li>Add all transport route edges with their capacities and unit costs.</li>
 *     </ul>
 *   </li>
 *   <li>Initialise Johnson potentials using Bellman-Ford on the initial graph.</li>
 *   <li>Repeat until no S→T augmenting path exists:
 *     <ul>
 *       <li>Run Dijkstra with reduced costs to find the cheapest augmenting path.</li>
 *       <li>Determine bottleneck flow on that path.</li>
 *       <li>Augment flow, update residual capacities and total cost.</li>
 *       <li>Update Johnson potentials for next iteration.</li>
 *     </ul>
 *   </li>
 *   <li>Extract per-route allocations from residual edge flows.</li>
 * </ol>
 *
 * <p>Time complexity: O(F · E log V) where F = number of augmenting paths.
 */
public class MCMFSolver {

    private static final Logger log = LoggerFactory.getLogger(MCMFSolver.class);
    private static final double INF = DijkstraSSSP.INF;
    private static final double EPS = 1e-6; // floating-point epsilon

    // ─── Input ─────────────────────────────────────────────────────────────────

    /** All supply records (depots / warehouses with stock) */
    private final List<InventoryRecord>  supplies;
    /** All demand records (FPS / distribution centres) */
    private final List<DemandRecord>     demands;
    /** Active transport routes */
    private final List<TransportRoute>   routes;
    /** Grain type being solved for */
    private final GrainType              grainType;
    /** Optional scenario name */
    private final String                 scenarioName;

    // ─── Graph indexing ────────────────────────────────────────────────────────

    /** locationId → graph node index */
    private final Map<Integer, Integer>  nodeIndex  = new LinkedHashMap<>();
    /** graph node index → locationId */
    private final Map<Integer, Integer>  indexToLoc = new LinkedHashMap<>();
    /** routeId → forward Edge object in graph */
    private final Map<Integer, Edge>     routeEdge  = new HashMap<>();

    private int superSource;
    private int superSink;

    // ─── Constructor ───────────────────────────────────────────────────────────

    public MCMFSolver(List<InventoryRecord> supplies,
                      List<DemandRecord>    demands,
                      List<TransportRoute>  routes,
                      GrainType             grainType,
                      String                scenarioName) {
        this.supplies     = supplies;
        this.demands      = demands;
        this.routes       = routes;
        this.grainType    = grainType;
        this.scenarioName = scenarioName;
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs the MCMF algorithm and returns a fully-populated {@link OptimizationResult}.
     */
    public OptimizationResult solve() {
        log.info("Starting MCMF for grain={}, scenario={}", grainType, scenarioName);
        long startMs = System.currentTimeMillis();

        // Step 1: Build graph
        FlowGraph graph = buildGraph();

        // Step 2: Initialise potentials via Bellman-Ford
        double[] potential = bellmanFordInit(graph, superSource);

        // Step 3: SSP main loop
        double totalFlow  = 0;
        double totalCost  = 0;

        while (true) {
            // Find shortest (cheapest) augmenting path S → T
            DijkstraSSSP.ShortestPathResult sp =
                    DijkstraSSSP.run(graph, superSource, potential);

            if (!sp.reachable(superSink)) break; // no more augmenting paths

            // Reconstruct path and find bottleneck
            Edge[] path      = sp.reconstructPath(superSource, superSink);
            double bottleneck = Double.MAX_VALUE;
            for (Edge e : path) {
                bottleneck = Math.min(bottleneck, e.cap);
            }

            if (bottleneck < EPS) break; // numerical safety

            // Augment flow along the path
            double pathCost = 0;
            for (Edge e : path) {
                e.cap -= bottleneck;
                graph.getReverse(0, e).cap += bottleneck;
                pathCost += e.cost * bottleneck;
            }

            totalFlow += bottleneck;
            totalCost += pathCost;

            // Update Johnson potentials
            potential = sp.updatedPotentials();
        }

        // Step 4: Extract per-route allocations and compute demand fulfilment
        long   durationMs = System.currentTimeMillis() - startMs;
        OptimizationResult result = buildResult(graph, totalFlow, totalCost, durationMs);
        log.info("MCMF complete: transported={:.2f} MT, cost=₹{:.2f}, time={}ms",
                totalFlow, totalCost, durationMs);
        return result;
    }

    // ─── Graph Construction ───────────────────────────────────────────────────

    private FlowGraph buildGraph() {
        // Collect unique location IDs from supplies + demands + routes
        Set<Integer> locIds = new LinkedHashSet<>();
        supplies.forEach(s -> locIds.add(s.getLocation().getId()));
        demands.forEach(d  -> locIds.add(d.getLocation().getId()));
        routes.forEach(r   -> {
            locIds.add(r.getSource().getId());
            locIds.add(r.getDestination().getId());
        });

        int idx = 0;
        for (int locId : locIds) {
            nodeIndex.put(locId, idx);
            indexToLoc.put(idx, locId);
            idx++;
        }

        // super-source and super-sink are the last two nodes
        superSource = idx;
        superSink   = idx + 1;
        int totalNodes = idx + 2;

        FlowGraph graph = new FlowGraph(totalNodes);

        // Supply edges: super-source → depot/warehouse
        for (InventoryRecord inv : supplies) {
            if (!inv.getGrainType().equals(grainType)) continue;
            double supplyMT = inv.getEffectiveSupplyMT();
            if (supplyMT < EPS) continue;
            int locNode = nodeIndex.get(inv.getLocation().getId());
            graph.addEdge(superSource, locNode, supplyMT, 0.0, 0);
        }

        // Demand edges: FPS/DC → super-sink
        for (DemandRecord dem : demands) {
            if (!dem.getGrainType().equals(grainType)) continue;
            double demandMT = dem.getRequiredMT();
            if (demandMT < EPS) continue;
            int locNode = nodeIndex.get(dem.getLocation().getId());
            graph.addEdge(locNode, superSink, demandMT, 0.0, 0);
        }

        // Route edges: source → destination with capacity and cost
        for (TransportRoute route : routes) {
            if (!route.isActive()) continue;
            Integer srcNode  = nodeIndex.get(route.getSource().getId());
            Integer dstNode  = nodeIndex.get(route.getDestination().getId());
            if (srcNode == null || dstNode == null) continue;

            Edge forward = graph.addEdge(srcNode, dstNode,
                    route.getCapacityMT(), route.getUnitCostPerMT(), route.getId());
            routeEdge.put(route.getId(), forward);
        }

        log.debug("Graph built: {} nodes, {} active routes", totalNodes, routes.size());
        return graph;
    }

    // ─── Bellman-Ford Initialisation for Johnson Potentials ──────────────────

    /**
     * Initialises Johnson potentials using Bellman-Ford from source.
     * Required before Dijkstra with reduced costs can guarantee correctness.
     * Runs in O(V·E) — acceptable for initialisation only.
     */
    private double[] bellmanFordInit(FlowGraph graph, int source) {
        int      n    = graph.getNodeCount();
        double[] dist = new double[n];
        Arrays.fill(dist, INF);
        dist[source] = 0.0;

        for (int iter = 0; iter < n - 1; iter++) {
            boolean updated = false;
            for (int u = 0; u < n; u++) {
                if (dist[u] >= INF) continue;
                for (Edge e : graph.getAdj(u)) {
                    if (e.cap > EPS && dist[u] + e.cost < dist[e.to] - EPS) {
                        dist[e.to] = dist[u] + e.cost;
                        updated    = true;
                    }
                }
            }
            if (!updated) break; // early termination
        }

        return dist;
    }

    // ─── Result Extraction ────────────────────────────────────────────────────

    private OptimizationResult buildResult(FlowGraph graph, double totalFlow,
                                           double totalCost, long durationMs) {
        OptimizationResult result = new OptimizationResult();
        result.setScenarioName(scenarioName);
        result.setGrainType(grainType);
        result.setSolveDurationMs(durationMs);

        // Total supply and demand
        double totalSupply = supplies.stream()
                .filter(s -> s.getGrainType().equals(grainType))
                .mapToDouble(InventoryRecord::getEffectiveSupplyMT)
                .sum();
        double totalDemand = demands.stream()
                .filter(d -> d.getGrainType().equals(grainType))
                .mapToDouble(DemandRecord::getRequiredMT)
                .sum();

        result.setTotalSupplyMT(totalSupply);
        result.setTotalDemandMT(totalDemand);
        result.setTotalTransportedMT(totalFlow);
        result.setTotalCost(totalCost);

        // Per-route allocations
        for (TransportRoute route : routes) {
            if (!route.isActive()) continue;
            Edge e = routeEdge.get(route.getId());
            if (e == null) continue;

            double flow = e.getFlow();
            if (flow < EPS) continue;

            AllocationResult alloc = new AllocationResult(0, route, flow, grainType);
            result.addAllocation(alloc);
        }

        double totalAllocationCost = result.getAllocations().stream()
                .mapToDouble(AllocationResult::getCostContribution)
                .sum();
        result.setTotalCost(totalAllocationCost);

        // Compute fulfilled demand: sum of flow on demand edges (into super-sink)
        double fulfilledDemand = 0;
        for (DemandRecord dem : demands) {
            if (!dem.getGrainType().equals(grainType)) continue;
            Integer locNode = nodeIndex.get(dem.getLocation().getId());
            if (locNode == null) continue;

            // Sum flow from this node to superSink
            double inflow = graph.getAdj(locNode).stream()
                    .filter(e -> e.to == superSink && e.routeId == 0)
                    .mapToDouble(Edge::getFlow)
                    .sum();
            fulfilledDemand += inflow;
            dem.setFulfilledMT(inflow);
        }

        result.setFulfilledDemandMT(fulfilledDemand);
        result.setUnmetDemandMT(Math.max(0, totalDemand - fulfilledDemand));

        double efficiency = (totalDemand > EPS)
                ? (fulfilledDemand / totalDemand) * 100.0 : 0.0;
        result.setNetworkEfficiencyPct(efficiency);

        if (totalFlow < EPS) {
            result.setStatus("INFEASIBLE");
        } else if (result.getUnmetDemandMT() > EPS) {
            result.setStatus("PARTIAL");
        } else {
            result.setStatus("SUCCESS");
        }

        return result;
    }
}
