package com.pds.logistics.service;

import com.pds.logistics.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CSV Export Service Tests")
class CsvExportServiceTest {

    @Test
    @DisplayName("Export single optimization result creates valid non-empty CSV")
    void testExportResult(@TempDir Path tempDir) throws IOException {
        CsvExportService service = new CsvExportService();

        Location src = new Location(1, "DEP-1", "Depot One", LocationType.DEPOT, "Pune", "MH", 18.5, 73.8);
        Location dst = new Location(2, "FPS-1", "FPS Alpha", LocationType.FAIR_PRICE_SHOP, "Pune", "MH", 18.6, 73.9);
        TransportRoute route = new TransportRoute(1, src, dst, 100.0, 15.0, 20.0, "ROAD");

        AllocationResult alloc = new AllocationResult();
        alloc.setRoute(route);
        alloc.setAllocatedMT(50.0);
        alloc.setCostContribution(750.0);

        OptimizationResult result = new OptimizationResult();
        result.setId(42);
        result.setScenarioName("Baseline Test");
        result.setGrainType(GrainType.RICE);
        result.setRunTimestamp(LocalDateTime.now());
        result.setAlgorithmUsed("Min-Cost Max-Flow (SSP + Dijkstra)");
        result.setStatus("OPTIMAL");
        result.setTotalSupplyMT(100.0);
        result.setTotalDemandMT(50.0);
        result.setTotalTransportedMT(50.0);
        result.setTotalCost(750.0);
        result.setFulfilledDemandMT(50.0);
        result.setUnmetDemandMT(0.0);
        result.setNetworkEfficiencyPct(100.0);
        result.setSolveDurationMs(12);
        result.setAllocations(List.of(alloc));

        Path targetCsv = tempDir.resolve("test_export.csv");
        service.exportResult(result, targetCsv.toString());

        assertTrue(Files.exists(targetCsv), "CSV file should be created");
        List<String> lines = Files.readAllLines(targetCsv);
        assertFalse(lines.isEmpty(), "CSV should not be empty");

        boolean foundHeader = lines.stream().anyMatch(l -> l.contains("PDS LOGISTICS OPTIMIZATION REPORT"));
        boolean foundScenario = lines.stream().anyMatch(l -> l.contains("Baseline Test"));
        boolean foundAllocation = lines.stream().anyMatch(l -> l.contains("DEP-1") && l.contains("FPS-1"));

        assertTrue(foundHeader, "CSV should contain report header");
        assertTrue(foundScenario, "CSV should contain scenario name");
        assertTrue(foundAllocation, "CSV should contain route allocation record");
    }
}
