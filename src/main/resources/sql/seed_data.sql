-- =============================================================================
-- PDS LOGISTICS SYSTEM – Realistic Seed Data (Maharashtra State)
-- 3 FCI Depots → 6 Buffer Warehouses → 18 Fair Price Shops
-- File: seed_data.sql
-- Run AFTER schema.sql
-- =============================================================================

USE pds_logistics;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE allocations;
TRUNCATE TABLE optimization_runs;
TRUNCATE TABLE demand_records;
TRUNCATE TABLE inventory;
TRUNCATE TABLE transport_routes;
TRUNCATE TABLE locations;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. LOCATIONS
-- =============================================================================
-- FCI Depots (Supply Sources)
INSERT INTO locations (id, code, name, type, district, state, latitude, longitude, canvas_x, canvas_y) VALUES
(1,  'FCI-MH-01', 'FCI Godown Pune',        'DEPOT', 'Pune',       'Maharashtra', 18.5204, 73.8567, 0.35, 0.45),
(2,  'FCI-MH-02', 'FCI Godown Nagpur',       'DEPOT', 'Nagpur',     'Maharashtra', 21.1458, 79.0882, 0.75, 0.20),
(3,  'FCI-MH-03', 'FCI Godown Aurangabad',   'DEPOT', 'Aurangabad', 'Maharashtra', 19.8762, 75.3433, 0.55, 0.35);

-- Buffer Warehouses (Intermediate nodes)
INSERT INTO locations (id, code, name, type, district, state, latitude, longitude, canvas_x, canvas_y) VALUES
(4,  'WH-MH-01', 'District Warehouse Nashik',      'WAREHOUSE', 'Nashik',     'Maharashtra', 19.9975, 73.7898, 0.28, 0.28),
(5,  'WH-MH-02', 'District Warehouse Solapur',     'WAREHOUSE', 'Solapur',    'Maharashtra', 17.6599, 75.9064, 0.40, 0.60),
(6,  'WH-MH-03', 'District Warehouse Amravati',    'WAREHOUSE', 'Amravati',   'Maharashtra', 20.9320, 77.7523, 0.68, 0.28),
(7,  'WH-MH-04', 'District Warehouse Kolhapur',    'WAREHOUSE', 'Kolhapur',   'Maharashtra', 16.7050, 74.2433, 0.22, 0.72),
(8,  'WH-MH-05', 'District Warehouse Nanded',      'WAREHOUSE', 'Nanded',     'Maharashtra', 19.1383, 77.3210, 0.62, 0.48),
(9,  'WH-MH-06', 'District Warehouse Jalgaon',     'WAREHOUSE', 'Jalgaon',    'Maharashtra', 21.0077, 75.5626, 0.45, 0.18);

-- Fair Price Shops (Demand Sinks)
INSERT INTO locations (id, code, name, type, district, state, latitude, longitude, canvas_x, canvas_y) VALUES
(10, 'FPS-PU-01', 'FPS Hadapsar Pune',           'FAIR_PRICE_SHOP', 'Pune',     'Maharashtra', 18.5010, 73.9356, 0.33, 0.48),
(11, 'FPS-PU-02', 'FPS Pimpri Pune',             'FAIR_PRICE_SHOP', 'Pune',     'Maharashtra', 18.6298, 73.7997, 0.30, 0.43),
(12, 'FPS-NK-01', 'FPS Nashik Road',             'FAIR_PRICE_SHOP', 'Nashik',   'Maharashtra', 19.9613, 73.8208, 0.26, 0.31),
(13, 'FPS-NK-02', 'FPS Malegaon',                'FAIR_PRICE_SHOP', 'Nashik',   'Maharashtra', 20.5579, 74.5313, 0.32, 0.24),
(14, 'FPS-SL-01', 'FPS Solapur City',            'FAIR_PRICE_SHOP', 'Solapur',  'Maharashtra', 17.6800, 75.9100, 0.38, 0.63),
(15, 'FPS-SL-02', 'FPS Barshi',                  'FAIR_PRICE_SHOP', 'Solapur',  'Maharashtra', 18.2330, 75.6900, 0.36, 0.56),
(16, 'FPS-AM-01', 'FPS Amravati City',           'FAIR_PRICE_SHOP', 'Amravati', 'Maharashtra', 20.9200, 77.7700, 0.66, 0.31),
(17, 'FPS-AM-02', 'FPS Achalpur',                'FAIR_PRICE_SHOP', 'Amravati', 'Maharashtra', 21.2564, 77.5124, 0.64, 0.23),
(18, 'FPS-KL-01', 'FPS Kolhapur City',           'FAIR_PRICE_SHOP', 'Kolhapur', 'Maharashtra', 16.7000, 74.2200, 0.20, 0.74),
(19, 'FPS-KL-02', 'FPS Ichalkaranji',            'FAIR_PRICE_SHOP', 'Kolhapur', 'Maharashtra', 16.6922, 74.4588, 0.24, 0.77),
(20, 'FPS-ND-01', 'FPS Nanded City',             'FAIR_PRICE_SHOP', 'Nanded',   'Maharashtra', 19.1500, 77.3100, 0.60, 0.51),
(21, 'FPS-ND-02', 'FPS Latur',                   'FAIR_PRICE_SHOP', 'Latur',    'Maharashtra', 18.4088, 76.5604, 0.52, 0.60),
(22, 'FPS-NG-01', 'FPS Nagpur City',             'FAIR_PRICE_SHOP', 'Nagpur',   'Maharashtra', 21.1458, 79.0882, 0.73, 0.22),
(23, 'FPS-NG-02', 'FPS Wardha',                  'FAIR_PRICE_SHOP', 'Wardha',   'Maharashtra', 20.7453, 78.5984, 0.70, 0.30),
(24, 'FPS-JL-01', 'FPS Jalgaon City',            'FAIR_PRICE_SHOP', 'Jalgaon',  'Maharashtra', 21.0100, 75.5700, 0.43, 0.21),
(25, 'FPS-JL-02', 'FPS Bhusawal',                'FAIR_PRICE_SHOP', 'Jalgaon',  'Maharashtra', 21.0432, 75.7895, 0.47, 0.19),
(26, 'FPS-AU-01', 'FPS Aurangabad City',         'FAIR_PRICE_SHOP', 'Aurangabad','Maharashtra',19.8762, 75.3433, 0.53, 0.38),
(27, 'FPS-AU-02', 'FPS Jalna',                   'FAIR_PRICE_SHOP', 'Jalna',    'Maharashtra', 19.8347, 75.8800, 0.57, 0.40);

-- =============================================================================
-- 2. TRANSPORT ROUTES (Depot → Warehouse → FPS)
--    Costs in ₹/MT, Capacities in MT
-- =============================================================================

-- From FCI Pune (1) to Warehouses
INSERT INTO transport_routes (source_id, destination_id, capacity_mt, unit_cost_per_mt, distance_km, transport_mode) VALUES
(1, 4, 2500, 450,  210, 'ROAD'),   -- Pune → Nashik
(1, 5, 3000, 380,  240, 'ROAD'),   -- Pune → Solapur
(1, 7, 2000, 520,  380, 'ROAD'),   -- Pune → Kolhapur
(1, 9, 1500, 490,  340, 'ROAD');   -- Pune → Jalgaon

-- From FCI Nagpur (2) to Warehouses
INSERT INTO transport_routes (source_id, destination_id, capacity_mt, unit_cost_per_mt, distance_km, transport_mode) VALUES
(2, 6, 2800, 320,  160, 'ROAD'),   -- Nagpur → Amravati
(2, 8, 2200, 510,  290, 'ROAD'),   -- Nagpur → Nanded
(2, 9, 1800, 430,  370, 'ROAD'),   -- Nagpur → Jalgaon
(2, 6, 3000, 290,  160, 'RAIL');   -- Nagpur → Amravati (Rail alternate)

-- From FCI Aurangabad (3) to Warehouses
INSERT INTO transport_routes (source_id, destination_id, capacity_mt, unit_cost_per_mt, distance_km, transport_mode) VALUES
(3, 4, 1200, 480,  230, 'ROAD'),   -- Aurangabad → Nashik
(3, 5, 1500, 420,  310, 'ROAD'),   -- Aurangabad → Solapur
(3, 8, 2000, 360,  190, 'ROAD'),   -- Aurangabad → Nanded
(3, 9, 1800, 350,  140, 'ROAD');   -- Aurangabad → Jalgaon

-- Warehouse → FPS routes
INSERT INTO transport_routes (source_id, destination_id, capacity_mt, unit_cost_per_mt, distance_km, transport_mode) VALUES
-- Nashik WH → FPS
(4, 12, 900,  240,  18, 'ROAD'),
(4, 13, 600,  310,  65, 'ROAD'),
(4, 24, 500,  290,  50, 'ROAD'),

-- Solapur WH → FPS
(5, 14, 1000, 180,   5, 'ROAD'),
(5, 15, 700,  250,  60, 'ROAD'),
(5, 21, 600,  320, 110, 'ROAD'),

-- Amravati WH → FPS
(6, 16, 900,  190,  12, 'ROAD'),
(6, 17, 500,  270,  40, 'ROAD'),
(6, 23, 600,  340,  80, 'ROAD'),

-- Kolhapur WH → FPS
(7, 18, 700,  160,   8, 'ROAD'),
(7, 19, 500,  220,  30, 'ROAD'),

-- Nanded WH → FPS
(8, 20, 800,  200,  10, 'ROAD'),
(8, 21, 700,  280,  80, 'ROAD'),

-- Jalgaon WH → FPS
(9, 24, 600,  230,  18, 'ROAD'),
(9, 25, 500,  250,  28, 'ROAD'),

-- Additional direct depot to FPS (bypass warehouse for nearby)
(1, 10, 500,  280,  20, 'ROAD'),   -- Pune → Hadapsar FPS
(1, 11, 400,  270,  22, 'ROAD'),   -- Pune → Pimpri FPS
(2, 22, 800,  210,  12, 'ROAD'),   -- Nagpur → Nagpur City FPS
(3, 26, 600,  230,  10, 'ROAD'),   -- Aurangabad → Aurangabad FPS
(3, 27, 400,  290,  65, 'ROAD'),   -- Aurangabad → Jalna FPS

-- Cross-warehouse transfers (for flexibility)
(4, 5, 800, 580, 420, 'ROAD'),
(6, 8, 600, 440, 280, 'ROAD');

-- =============================================================================
-- 3. INVENTORY (Supply stocks as of current period)
-- =============================================================================
INSERT INTO inventory (location_id, grain_type, quantity_mt, allocated_mt, report_date) VALUES
-- FCI Pune stocks
(1, 'WHEAT', 8500, 0, CURDATE()),
(1, 'RICE',  6200, 0, CURDATE()),

-- FCI Nagpur stocks
(2, 'WHEAT', 7200, 0, CURDATE()),
(2, 'RICE',  5800, 0, CURDATE()),

-- FCI Aurangabad stocks
(3, 'WHEAT', 5500, 0, CURDATE()),
(3, 'RICE',  4200, 0, CURDATE());

-- =============================================================================
-- 4. DEMAND RECORDS (Current period – monthly allocation)
-- =============================================================================
INSERT INTO demand_records (location_id, grain_type, required_mt, beneficiary_count, period_start, period_end) VALUES
-- FPS Pune area
(10, 'WHEAT', 320, 12800, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(10, 'RICE',  280, 12800, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(11, 'WHEAT', 290, 11600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(11, 'RICE',  260, 11600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Nashik
(12, 'WHEAT', 380, 15200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(12, 'RICE',  340, 15200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(13, 'WHEAT', 240, 9600,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(13, 'RICE',  200, 9600,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Solapur
(14, 'WHEAT', 450, 18000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(14, 'RICE',  400, 18000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(15, 'WHEAT', 280, 11200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(15, 'RICE',  250, 11200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Amravati
(16, 'WHEAT', 360, 14400, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(16, 'RICE',  320, 14400, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(17, 'WHEAT', 180, 7200,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(17, 'RICE',  160, 7200,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Kolhapur
(18, 'WHEAT', 420, 16800, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(18, 'RICE',  380, 16800, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(19, 'WHEAT', 260, 10400, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(19, 'RICE',  230, 10400, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Nanded
(20, 'WHEAT', 340, 13600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(20, 'RICE',  300, 13600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(21, 'WHEAT', 290, 11600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(21, 'RICE',  260, 11600, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Nagpur
(22, 'WHEAT', 500, 20000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(22, 'RICE',  450, 20000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(23, 'WHEAT', 220, 8800,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(23, 'RICE',  200, 8800,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Jalgaon
(24, 'WHEAT', 300, 12000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(24, 'RICE',  270, 12000, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(25, 'WHEAT', 200, 8000,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(25, 'RICE',  180, 8000,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),

-- FPS Aurangabad
(26, 'WHEAT', 380, 15200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(26, 'RICE',  340, 15200, DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(27, 'WHEAT', 210, 8400,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW())),
(27, 'RICE',  190, 8400,  DATE_FORMAT(NOW(),'%Y-%m-01'), LAST_DAY(NOW()));

-- =============================================================================
-- Verify row counts
-- =============================================================================
SELECT 'Locations'       AS entity, COUNT(*) AS count FROM locations
UNION ALL
SELECT 'Routes',          COUNT(*) FROM transport_routes
UNION ALL
SELECT 'Inventory',       COUNT(*) FROM inventory
UNION ALL
SELECT 'Demand records',  COUNT(*) FROM demand_records;
