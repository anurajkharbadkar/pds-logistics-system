# Public Distribution System (PDS) Logistics Optimization & Decision Support System
## College Project Presentation, Architecture & Viva Voce Guide

---

## 1. Project Identification & Metadata

- **Project Title:** PDS Food-Grain Logistics Optimization & Decision Support System
- **Domain:** Public Distribution System (PDS) Supply Chain Logistics, Network Flow Algorithms, Operations Research
- **Student Name:** Anuraj Kharbadkar
- **Target Platform:** Desktop Decision Support System (GUI)
- **Technology Stack:**
  - **Programming Language:** Java 21 (LTS)
  - **GUI Toolkit:** JavaFX 21 & FXML, CSS3 Modern Dark Theme
  - **Algorithms:** Min-Cost Max-Flow (MCMF) using Successive Shortest Path (SSP) with Johnson's Node Potentials & Dijkstra SSSP
  - **Database Management System:** MySQL 8.x / MariaDB (InnoDB Engine, ACID compliant, JDBC)
  - **Data Interchange & Reports:** OpenCSV, Tabular Exports
  - **Testing & Quality Assurance:** JUnit 5 (12 automated unit tests across core algorithms and DAOs)
  - **Build Automation & Containerization:** Apache Maven 3.x, Docker Compose

---

## 2. Problem Statement & Motivation

The **Public Distribution System (PDS)** in India is the world's largest food security network, distributing subsidized staple grains (wheat, rice, coarse grains) to over 800 million citizens via a hierarchical 3-tier logistics network:
$$\text{FCI Depots (Base Storage)} \longrightarrow \text{State Warehouses (Buffer Stocks)} \longrightarrow \text{District Distribution Centers} \longrightarrow \text{Fair Price Shops (FPS)}$$

### Real-World Operational Inefficiencies:
1. **Sub-optimal Route Allocation:** Transportation allocations are traditionally calculated using static rules or regional quotas, generating sub-optimal multi-leg routings that escalate public expenditure.
2. **Artificial Deficits & Glut:** Lack of globally optimal flow planning leads to excess buffer inventory in some districts while remote fair price shops experience artificial stockouts.
3. **Fragility to Disruptions:** Corridors frequently shut down due to monsoon flooding, road repairs, or geopolitical events. Manual replanning takes days.
4. **Demand Spikes:** Seasonal festival entitlements require agile dynamic reallocation within vehicle capacity bounds.

### Proposed Solution:
An interactive **Decision Support System (DSS)** that models the end-to-end multi-tier logistics network as a **Directed Flow Network** and computes the globally minimum transportation expenditure while ensuring 100% demand satisfaction using the **Min-Cost Max-Flow (MCMF)** algorithm.

---

## 3. Mathematical Formulation

The supply chain is modeled as a directed graph $G = (V, E)$:

### 3.1 Graph Vertices ($V$)
- $V_{\text{depot}}$: Base food storage godowns (FCI Pune, FCI Nagpur, FCI Aurangabad).
- $V_{\text{warehouse}}$: District buffer warehouses (Nashik, Solapur, Amravati, Kolhapur, Nanded, Jalgaon).
- $V_{\text{fps}}$: Last-mile Fair Price Shops (18 consumption nodes across Maharashtra).
- **Super-Source ($S$):** Virtual vertex connected to all depots $u \in V_{\text{depot}}$ with:
  $$\text{Capacity}(S \to u) = \text{AvailableStock}(u), \quad \text{Cost}(S \to u) = 0$$
- **Super-Sink ($T$):** Virtual vertex connected from all Fair Price Shops $v \in V_{\text{fps}}$ with:
  $$\text{Capacity}(v \to T) = \text{MonthlyQuota}(v), \quad \text{Cost}(v \to T) = 0$$

### 3.2 Optimization Objective Function
$$\min \sum_{(u,v) \in E} c(u, v) \cdot f(u, v)$$

Where:
- $f(u, v)$ is the flow (in Metric Tonnes, MT) scheduled on edge $(u, v)$.
- $c(u, v)$ is the unit transportation cost (in INR/MT) for route $(u, v)$.

### 3.3 Governing Constraints
1. **Capacity Bounds:**
   $$0 \le f(u, v) \le \text{Capacity}(u, v), \quad \forall (u, v) \in E$$
2. **Conservation of Flow (Kirchhoff's Current Law):**
   $$\sum_{w \in V} f(v, w) - \sum_{u \in V} f(u, v) = 0, \quad \forall v \in V \setminus \{S, T\}$$
3. **Demand Satisfaction:**
   $$\sum_{u \in V} f(u, v) = \text{Demand}(v), \quad \forall v \in V_{\text{fps}} \quad (\text{when total supply} \ge \text{total demand})$$

---

## 4. Algorithm Engine: Successive Shortest Path (SSP) with Johnson's Potentials

### 4.1 Why Standard Ford-Fulkerson or Bellman-Ford Alone is Insufficient
In residual graphs of minimum-cost flow, reverse edges have negative marginal cost ($c(e_{\text{rev}}) = -c(e)$). Running Bellman-Ford on every augmentation step takes $O(V \cdot E)$ per path, which becomes a severe computational bottleneck for large-scale logistics networks.

### 4.2 Johnson's Node Potential Transformation
To use **Dijkstra's Algorithm with Fibonacci/Priority Queue** ($O(E \log V)$) on graphs with negative edge weights:
1. Assign a potential $h(u)$ to each vertex $u \in V$.
2. Define the **Reduced Cost** $\hat{c}(u, v)$ of edge $(u, v)$:
   $$\hat{c}(u, v) = c(u, v) + h(u) - h(v)$$
3. **Invariance Property:** For any path $P$ from $S$ to $T$:
   $$\sum_{e \in P} \hat{c}(e) = \sum_{e \in P} c(e) + h(S) - h(T)$$
   The total cost of all $S-T$ paths is shifted by the identical constant $h(S) - h(T)$, preserving shortest paths.
4. **Non-negativity Guarantee:**
   Using potentials derived from distance labels:
   $$\hat{c}(u, v) \ge 0$$
   This enables Dijkstra's $O(E \log V)$ priority queue algorithm to run safely without cycle degradation.
5. **Potential Update Rule:**
   After each Dijkstra augmentation run:
   $$h_{\text{new}}(u) = h_{\text{old}}(u) + \text{dist}(u)$$

---

## 5. System Architecture & Modular Design

```
+-----------------------------------------------------------------------------------+
|                            JavaFX Presentation Tier                               |
|  - MainWindowController    - DashboardController   - NetworkCanvas (2D Flow Graph)|
|  - LocationsController     - RoutesController      - ScenariosController          |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                               Service & Algorithm Tier                            |
|  - OptimizationService     - MCMFSolver (SSP + Johnson)   - DijkstraSSSP          |
|  - FlowGraph / Edge Models - Scenario Parameter Engine    - CsvExportService      |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                             JDBC Data Access Tier                                 |
|  - LocationDAO             - RouteDAO              - InventoryDAO                 |
|  - OptimizationDAO         - DatabaseConfig (Connection Lifecycle Pool)           |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                               Storage Tier (RDBMS)                                |
|  MySQL 8.x / MariaDB Database (locations, transport_routes, inventory, demands)   |
+-----------------------------------------------------------------------------------+
```

---

## 6. What-If Scenario Analysis Capabilities

The system allows supply chain officers to execute real-time simulation experiments:
1. **Baseline / Normal Allocation:** Optimal distribution under standard capacity and demand conditions.
2. **Monsoon / Highway Disruption:** Dynamically marks high-risk transport corridors (e.g., Pune-Nashik NH-60) as inactive and reroutes flow via alternate warehouses with minimum cost degradation.
3. **Festival / Emergency Demand Surge:** Increases Fair Price Shop demand quotas by +25% to test network stress and identify bottleneck routes.
4. **Fuel Price Shock (+20% Unit Transit Cost):** Simulates diesel price increases across road transport modes to recompute fiscal budget requirements.

---

## 7. Viva Voce / Project Defense Q&A

### Q1: Why did you choose Min-Cost Max-Flow over the standard Transportation Simplex algorithm?
**Answer:** The standard Transportation Simplex assumes a bipartite graph directly linking sources to sinks. In national logistics such as PDS, goods traverse **multiple intermediate tiers** (Depot $\to$ Warehouse $\to$ FPS) with intermediate capacity bottlenecks and multimodal edges. MCMF on a general flow network models multi-tier intermediate vertices, route capacity constraints, and edge directions naturally.

### Q2: What is the computational time complexity of your MCMF solver?
**Answer:** Using the Successive Shortest Path (SSP) formulation with Johnson Potentials:
- Initial potential generation: Bellman-Ford takes $O(V \cdot E)$.
- Each augmenting step: Dijkstra with a Binary Heap takes $O(E \log V)$.
- If $F$ is the maximum total flow units augmented, the total runtime is $O(V \cdot E + F \cdot E \log V)$. For our state-level network with 27 nodes and 34 routes, the optimization runs in **under 15 milliseconds**, allowing real-time interactive GUI recalculations.

### Q3: How do you prevent negative cost cycles during flow augmentation?
**Answer:** By the theorem of Klein and Busacker-Gowen, augmenting flow along the shortest path in the residual network with respect to reduced costs preserves the optimality condition (no negative-cost directed cycles in the residual graph). Johnson's potential transformation ensures that $\hat{c}(u, v) \ge 0$ at every step.

### Q4: How is database persistence structured?
**Answer:** We adhere to the Data Access Object (DAO) pattern. Pure SQL queries are encapsulated within `LocationDAO`, `RouteDAO`, `InventoryDAO`, and `OptimizationDAO`, with transactional integrity guaranteed through JDBC batch operations and prepared statements to prevent SQL injection.

### Q5: How can this system be scaled up to national level (e.g., all 28 Indian States)?
**Answer:**
1. Hierarchical Decomposition: Decompose the graph regionally into State sub-networks with inter-state FCI trunk lines modeled as macro-edges.
2. Capacity Scaling / Cost Scaling MCMF: Implement push-relabel or cost-scaling algorithms ($O(V^2 E \log(V \cdot C))$) to handle millions of nodes in polynomial time independent of total flow $F$.
3. GIS Integration: Replace 2D canvas coordinates with Leaflet/OpenStreetMap GIS layers.
