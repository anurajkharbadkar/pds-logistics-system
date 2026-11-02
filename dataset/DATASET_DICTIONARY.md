# PDS Logistics Optimization System – Dataset Dictionary & Data Model

This directory contains the complete reference dataset used for benchmarking and demonstrating the Public Distribution System (PDS) food-grain logistics optimization engine. The dataset models the supply chain of Maharashtra State, India, spanning Food Corporation of India (FCI) base depots, state buffer warehouses, and fair price shops (FPS).

---

## 1. Dataset Files Overview

| File Name | Description | Rows / Records | Format |
|---|---|---|---|
| [`locations.csv`](./locations.csv) | Supply chain nodes: FCI Depots, Buffer Warehouses, and Fair Price Shops | 27 facilities | CSV |
| [`transport_routes.csv`](./transport_routes.csv) | Directed transportation corridors with transit capacities, costs, and distances | 34 routes | CSV |
| [`inventory.csv`](./inventory.csv) | Stock levels available at base supply depots by commodity (Wheat, Rice) | 6 stock records | CSV |
| [`demand_records.csv`](./demand_records.csv) | Monthly quota demand and beneficiary counts across Fair Price Shops | 36 monthly quotas | CSV |
| [`schema.sql`](../src/main/resources/sql/schema.sql) | DDL script defining tables, foreign keys, and indexes | MySQL 8.x schema | SQL |
| [`seed_data.sql`](../src/main/resources/sql/seed_data.sql) | DML script to populate all tables in MySQL / MariaDB | Full initial seed | SQL |

---

## 2. Table Schemas & Data Dictionaries

### 2.1 Locations (`locations.csv`)
Represents physical facilities (nodes $V$) in the logistics graph.

| Column | Data Type | Constraints | Description | Sample Value |
|---|---|---|---|---|
| `id` | `INT` | PRIMARY KEY | Unique location ID | `1` |
| `code` | `VARCHAR(20)` | UNIQUE, NOT NULL | Standard administrative facility code | `FCI-MH-01` |
| `name` | `VARCHAR(120)` | NOT NULL | Human-readable name of the facility | `FCI Godown Pune` |
| `type` | `ENUM` | NOT NULL | Hierarchy level: `DEPOT`, `WAREHOUSE`, `FAIR_PRICE_SHOP` | `DEPOT` |
| `district` | `VARCHAR(80)` | NULLABLE | District name in Maharashtra | `Pune` |
| `state` | `VARCHAR(60)` | DEFAULT 'Maharashtra' | State name | `Maharashtra` |
| `latitude` | `DOUBLE` | NOT NULL | Geographic latitude coordinate (WGS84) | `18.5204` |
| `longitude`| `DOUBLE` | NOT NULL | Geographic longitude coordinate (WGS84) | `73.8567` |
| `canvas_x` | `DOUBLE` | NOT NULL | Normalized [0, 1] X coordinate for 2D UI canvas | `0.35` |
| `canvas_y` | `DOUBLE` | NOT NULL | Normalized [0, 1] Y coordinate for 2D UI canvas | `0.45` |
| `active`   | `BOOLEAN` | DEFAULT 1 (TRUE) | Operational status (1 = Active, 0 = Inactive) | `1` |

#### Location Hierarchy Breakdown:
- **3 FCI Base Depots (Tier-1 Supply Sources):** Pune (`FCI-MH-01`), Nagpur (`FCI-MH-02`), Aurangabad (`FCI-MH-03`).
- **6 State Warehouses (Tier-2 Buffer Transit Points):** Nashik (`WH-MH-01`), Solapur (`WH-MH-02`), Amravati (`WH-MH-03`), Kolhapur (`WH-MH-04`), Nanded (`WH-MH-05`), Jalgaon (`WH-MH-06`).
- **18 Fair Price Shops (Tier-3 Demand Sinks):** Distributed across Pune, Nashik, Solapur, Amravati, Kolhapur, Nanded, Latur, Nagpur, Wardha, Jalgaon, Aurangabad, and Jalna.

---

### 2.2 Transport Routes (`transport_routes.csv`)
Represents directed transportation links (edges $E$) between supply nodes and demand nodes.

| Column | Data Type | Constraints | Description | Sample Value |
|---|---|---|---|---|
| `id` | `INT` | PRIMARY KEY | Unique route ID | `1` |
| `source_id` | `INT` | FOREIGN KEY (`locations.id`) | Origin location ID | `1` (FCI Pune) |
| `destination_id` | `INT` | FOREIGN KEY (`locations.id`) | Destination location ID | `4` (WH Nashik) |
| `capacity_mt` | `DOUBLE` | NOT NULL, $\ge 0$ | Maximum transit throughput capacity in Metric Tonnes (MT) | `2500` |
| `unit_cost_per_mt` | `DOUBLE` | NOT NULL, $\ge 0$ | Cost in Indian Rupees (INR) to transport 1 MT along this route | `450` |
| `distance_km` | `DOUBLE` | NOT NULL, $\ge 0$ | Road travel distance in kilometers | `210` |
| `transport_mode` | `VARCHAR(10)` | DEFAULT 'ROAD' | Transit mode: `ROAD`, `RAIL`, `RIVER` | `ROAD` |
| `active` | `BOOLEAN` | DEFAULT 1 (TRUE) | Operational availability (toggleable in disruption scenarios) | `1` |
| `remarks` | `TEXT` | NULLABLE | Operational notes (e.g., Highway designation) | `NH-60 Corridor` |

---

### 2.3 Inventory (`inventory.csv`)
Represents grain stocks available at Tier-1 FCI base depots.

| Column | Data Type | Constraints | Description | Sample Value |
|---|---|---|---|---|
| `id` | `INT` | PRIMARY KEY | Unique inventory record ID | `1` |
| `location_id` | `INT` | FOREIGN KEY (`locations.id`) | FCI Depot holding the grain | `1` (FCI Pune) |
| `grain_type` | `ENUM` | NOT NULL | Grain category: `WHEAT`, `RICE`, `COARSE_GRAIN` | `WHEAT` |
| `quantity_mt` | `DOUBLE` | NOT NULL, $\ge 0$ | Total stock on hand in Metric Tonnes (MT) | `8500` |
| `allocated_mt` | `DOUBLE` | DEFAULT 0 | Quantity allocated to fulfill downstream demand | `0` |
| `report_date` | `DATE` | NOT NULL | Stock audit timestamp | `2026-11-02` |

---

### 2.4 Demand Records (`demand_records.csv`)
Represents monthly entitlement quotas at Tier-3 Fair Price Shops.

| Column | Data Type | Constraints | Description | Sample Value |
|---|---|---|---|---|
| `id` | `INT` | PRIMARY KEY | Unique demand record ID | `1` |
| `location_id` | `INT` | FOREIGN KEY (`locations.id`) | FPS requesting the grain | `10` (FPS Hadapsar Pune) |
| `grain_type` | `ENUM` | NOT NULL | Grain category: `WHEAT`, `RICE`, `COARSE_GRAIN` | `WHEAT` |
| `required_mt` | `DOUBLE` | NOT NULL, $\ge 0$ | Monthly sanctioned quota in Metric Tonnes (MT) | `320` |
| `fulfilled_mt`| `DOUBLE` | DEFAULT 0 | Quantity received / allocated by optimization engine | `0` |
| `period_start`| `DATE` | NOT NULL | Allocation cycle start date | `2026-11-01` |
| `period_end`  | `DATE` | NOT NULL | Allocation cycle end date | `2026-11-30` |
| `beneficiary_count` | `INT` | NOT NULL | Registered ration card beneficiaries served | `12800` |

---

## 3. Network Flow Mathematical Mapping

1. **Super-Source ($S$):** Virtual source vertex with directed edges to all Depot locations ($i \in \{1, 2, 3\}$).
   - Capacity: $\text{Capacity}(S \to i) = \text{Inventory}(i)$
   - Cost: $c(S \to i) = 0$
2. **Intermediate Flow:** Directed edges from Depots to Warehouses, and Warehouses to Fair Price Shops according to `transport_routes.csv`.
   - Capacity: $u(u \to v) = \text{capacity\_mt}$
   - Cost: $c(u \to v) = \text{unit\_cost\_per\_mt}$
3. **Super-Sink ($T$):** Virtual sink vertex receiving directed edges from all Fair Price Shops ($j \in \{10, \dots, 27\}$).
   - Capacity: $\text{Capacity}(j \to T) = \text{required\_mt}$
   - Cost: $c(j \to T) = 0$
