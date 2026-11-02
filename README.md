# PDS Logistics Optimization & Decision Support System

> **Academic Mini Project**  
> **Domain:** Public Distribution System (PDS) Food-Grain Logistics Optimization  
> **Technology Stack:** Java 21, JavaFX 21, MySQL 8, JDBC, Maven, OpenCSV, SLF4J, JUnit 5  

---

## 1. Executive Summary & Problem Formulation

The **Public Distribution System (PDS)** is India’s largest food security logistics network, distributing subsidized food grains (Rice, Wheat, Coarse Grains) to over 800 million citizens via a multi-tier hierarchy:
$$\text{FCI Base Depots} \longrightarrow \text{State Warehouses} \longrightarrow \text{District Distribution Centers} \longrightarrow \text{Fair Price Shops (FPS)}$$

In practice, manual and heuristic allocations lead to:
- Excessive transportation costs due to non-optimal route selection.
- Bottlenecked routes causing artificial deficits at fair price shops.
- Vulnerability to disruptions (bridge closures, seasonal supply variations, festival demand spikes).

### Core Objective
To develop an enterprise-grade desktop decision-support system that models the food-grain supply chain as a **directed flow network** and solves the **Min-Cost Max-Flow (MCMF)** problem to determine the globally optimal allocation of grain quantities across active transportation routes, minimizing total logistics expenditures while satisfying local demands and capacity constraints.

---

## 2. Theoretical Architecture & Algorithms

### 2.1 Network Flow Formulation
Let the logistics network be represented as a directed graph $G = (V, E)$:
- **Nodes ($V$):** Locations categorized as `DEPOT`, `WAREHOUSE`, `DISTRIBUTION_CENTER`, and `FAIR_PRICE_SHOP`.
- **Edges ($E$):** Transportation corridors with upper capacity $u(e) \ge 0$, unit transit cost $c(e) \ge 0$, and operational status.
- **Super-Source ($S$):** Connected to all supply nodes $v \in V_{\text{supply}}$ with capacity equal to available stock $\text{Supply}(v)$ and cost $0$.
- **Super-Sink ($T$):** Connected from all demand nodes $w \in V_{\text{demand}}$ with capacity equal to quota requirement $\text{Demand}(w)$ and cost $0$.

$$\min \sum_{e \in E} c(e) \cdot f(e)$$

Subject to:
1. **Capacity Constraints:** $0 \le f(e) \le u(e), \quad \forall e \in E$
2. **Conservation of Flow:** $\sum_{w: (v,w) \in E} f(v,w) - \sum_{u: (u,v) \in E} f(u,v) = 0, \quad \forall v \in V \setminus \{S, T\}$
3. **Non-negativity:** $f(e) \ge 0$

### 2.2 Successive Shortest Path (SSP) with Johnson Potentials
Standard MCMF algorithms like Ford-Fulkerson can be inefficient, and residual graphs contain negative-cost reverse edges ($c(e_{\text{rev}}) = -c(e)$). To achieve high computational efficiency:
1. **Initial Potentials:** Computed once at startup using **Bellman-Ford** from super-source $S$.
2. **Johnson Node Potentials ($h[v]$):** Every edge reduced cost is transformed to:
   $$\hat{c}(u, v) = c(u, v) + h[u] - h[v] \ge 0$$
3. **Dijkstra Single-Source Shortest Path (SSSP):** Because reduced costs $\hat{c} \ge 0$, **Dijkstra with Priority Queue** ($O(E \log V)$) is executed to discover the augmenting path with minimum marginal cost.
4. **Bottleneck Augmentation:** Residual capacities are updated along the augmenting path by $\delta = \min_{e \in P} r(e)$.
5. **Potential Updates:** Potentials are updated via $h'[v] = h[v] + \text{dist}[v]$.
6. **Termination:** Halts when no augmenting path with positive residual capacity exists between $S$ and $T$.

---

## 3. System Architecture & Project Structure

The project strictly follows layered enterprise architecture:

```
pds-logistics-system/
├── pom.xml                               # Maven project descriptor with JavaFX & MySQL
├── src/
│   ├── main/
│   │   ├── java/com/pds/logistics/
│   │   │   ├── MainApp.java              # JavaFX application launcher
│   │   │   ├── config/
│   │   │   │   └── DatabaseConfig.java   # Hikari-style connection pool & lifecycle
│   │   │   ├── model/                    # Domain POJOs
│   │   │   │   ├── Location.java         # Nodes (Depots, Warehouses, FPS)
│   │   │   │   ├── LocationType.java     # Enum of facility types
│   │   │   │   ├── GrainType.java        # WHEAT, RICE, COARSE_GRAIN
│   │   │   │   ├── TransportRoute.java   # Directed edges with capacity & unit costs
│   │   │   │   ├── InventoryRecord.java  # Stock levels at supply locations
│   │   │   │   ├── DemandRecord.java     # Monthly quotas at demand nodes
│   │   │   │   ├── OptimizationResult.java # Run outcome metrics
│   │   │   │   └── AllocationResult.java # Per-edge allocated quantities
│   │   │   ├── dao/                      # JDBC Data Access Objects
│   │   │   │   ├── LocationDAO.java
│   │   │   │   ├── RouteDAO.java
│   │   │   │   ├── InventoryDAO.java
│   │   │   │   └── OptimizationDAO.java
│   │   │   ├── service/                  # Business logic & background processing
│   │   │   │   ├── OptimizationService.java
│   │   │   │   └── CsvExportService.java
│   │   │   ├── algorithm/                # Optimization engine
│   │   │   │   ├── Edge.java             # Residual graph edge
│   │   │   │   ├── FlowGraph.java        # Adjacency list representation
│   │   │   │   ├── DijkstraSSSP.java     # Dijkstra with Johnson potentials
│   │   │   │   ├── MCMFSolver.java       # Min-Cost Max-Flow SSP solver
│   │   │   │   └── Scenario.java         # What-if scenario parameter manager
│   │   │   └── ui/                       # Modern JavaFX Presentation Tier
│   │   │       ├── components/
│   │   │       │   └── NetworkCanvas.java # Custom 2D network visualization
│   │   │       └── controllers/
│   │   │           ├── MainWindowController.java
│   │   │           ├── DashboardController.java
│   │   │           ├── LocationsController.java
│   │   │           ├── InventoryController.java
│   │   │           ├── DemandController.java
│   │   │           ├── RoutesController.java
│   │   │           ├── OptimizeController.java
│   │   │           ├── NetworkController.java
│   │   │           ├── ScenariosController.java
│   │   │           └── HistoryController.java
│   │   └── resources/
│   │       ├── css/
│   │       │   └── styles.css            # Sleek dark theme UI stylesheet
│   │       ├── fxml/                     # Declarative UI templates
│   │       │   ├── MainWindow.fxml
│   │       │   ├── Dashboard.fxml
│   │       │   ├── Locations.fxml
│   │       │   ├── Inventory.fxml
│   │       │   ├── Demand.fxml
│   │       │   ├── Routes.fxml
│   │       │   ├── Optimize.fxml
│   │       │   ├── Network.fxml
│   │       │   ├── Scenarios.fxml
│   │       │   └── History.fxml
│   │       ├── db.properties             # Database credentials
│   │       └── sql/
│   │           ├── schema.sql            # Normalized MySQL DDL tables
│   │           └── seed_data.sql         # Real-world benchmark dataset
│   └── test/java/com/pds/logistics/
│       ├── algorithm/
│       │   ├── DijkstraSSSPTest.java     # Dijkstra & Johnson potentials unit tests
│       │   ├── MCMFSolverTest.java       # MCMF optimization tests
│       │   └── ScenarioTest.java         # What-if perturbation tests
│       └── service/
│           └── CsvExportServiceTest.java # CSV report export tests
```

---

## 4. Key Application Features

1. **Executive KPI Dashboard:**
   - Real-time aggregation of active facilities, total grain inventory across state depots, demand at Fair Price Shops, net surplus/deficit, and summary metrics from the latest optimization run.

2. **Supply Chain Master Data Management:**
   - Dedicated management screens for **Locations**, **Inventory**, **Demand Quotas**, and **Transport Routes**.
   - Live database validation and asynchronous execution on Java 21 Virtual Threads (`Thread.ofVirtual()`), keeping the JavaFX UI completely non-blocking.

3. **Min-Cost Max-Flow Optimization Engine:**
   - Grain selection (`WHEAT`, `RICE`, `COARSE_GRAIN`).
   - One-click computation showing total transported quantity (MT), total logistical cost (INR), fulfilled demand %, unmet demand, efficiency rating, and computation latency.
   - Comprehensive allocation table displaying route-wise source, destination, allocated volume, unit rate, and total cost contribution.

4. **Interactive Network Canvas Visualizer:**
   - High-performance custom 2D canvas depicting facilities with geographic coordinates.
   - Node color-coding: FCI Depots (Blue), Warehouses (Teal), Distribution Centers (Purple), Fair Price Shops (Green/Orange).
   - Flow thickness scaled proportionally to allocated grain tonnage.
   - Interactive hover tooltips displaying facility capacity, current stock, and outgoing routes.

5. **What-If Scenario Decision Support:**
   - Simulates policy and physical disruptions:
     - **Supply Deficit:** Drought or procurement drop (e.g., -20% supply).
     - **Demand Surge:** Festival season or emergency relief (e.g., +30% demand).
     - **Route Failure:** Flood, landslide, or highway closure disabling specific transport links.
     - **Fuel Price Escalation:** Variable unit transport cost spikes.
   - Side-by-side comparative table contrasting baseline KPIs against scenario outcomes with delta indicators.

6. **Historical Audit & CSV Export:**
   - Complete persistence of every optimization run into MySQL with microsecond timestamps.
   - Multi-format CSV export for regulatory reporting, audits, and external analysis.

---

## 5. Getting Started & Installation

### Prerequisites
- **Java Development Kit (JDK):** Version 21 or higher.
- **Apache Maven:** Version 3.8 or higher.
- **MySQL Database Server:** Version 8.0 or higher.

### Step 1: Database Setup
1. Log into your MySQL console or MySQL Workbench:
   ```bash
   mysql -u root -p
   ```
2. Execute the schema script:
   ```sql
   source src/main/resources/sql/schema.sql;
   ```
3. Load the pre-configured realistic benchmark dataset:
   ```sql
   source src/main/resources/sql/seed_data.sql;
   ```

### Step 2: Database Configuration
Verify or edit `src/main/resources/db.properties` with your MySQL credentials:
```properties
db.url=jdbc:mysql://localhost:3306/pds_logistics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root
db.driver=com.mysql.cj.jdbc.Driver
```

### Step 3: Run Automated Tests
Execute the JUnit 5 test suite to verify algorithm correctness and report generation:
```bash
mvn test
```

### Step 4: Run the Application
Launch the JavaFX desktop application using the Maven JavaFX plugin:
```bash
mvn clean javafx:run
```
*(Or package an executable JAR using `mvn clean package`)*.

---

## 6. Verification & Test Suite Summary

The unit test suite covers key graph theory and system invariants:
- `DijkstraSSSPTest`: SSSP path recovery, positive reduced cost invariant under Johnson potentials, and zero-residual edge filtering.
- `MCMFSolverTest`: Optimal flow allocation, capacity bottleneck enforcement, multi-depot cost minimization, and inactive route exclusion.
- `ScenarioTest`: Deep-copy immutability, supply reduction, demand surge, and targeted edge failure.
- `CsvExportServiceTest`: CSV generation, header integrity, and allocation record validation.

---

## 7. College Presentation, Datasets & Viva Voce Resources

For academic project submission, evaluations, and external viva voce presentations, the following dedicated attachments are included:

- **[Raw Datasets & Dictionary](file://dataset/DATASET_DICTIONARY.md)**:
  - [`dataset/locations.csv`](file://dataset/locations.csv) – All 27 supply, warehouse, and demand nodes with geographic coordinates.
  - [`dataset/transport_routes.csv`](file://dataset/transport_routes.csv) – 34 transportation corridors with capacities and unit costs.
  - [`dataset/inventory.csv`](file://dataset/inventory.csv) – Depot commodity stock levels.
  - [`dataset/demand_records.csv`](file://dataset/demand_records.csv) – Monthly fair price shop quotas and beneficiary counts.
- **[College Presentation & Viva Voce Guide](file://docs/COLLEGE_PRESENTATION_AND_VIVA_GUIDE.md)** – Comprehensive theoretical proofs, time complexity derivations, architecture diagrams, and anticipated viva questions with answers.
- **[Presentation Slide Brief](file://docs/PROJECT_POSTER_SUMMARY.md)** – Elevator pitch, demo checklist, and key metrics for slides and presentation posters.

