package com.pds.logistics.service;

import com.opencsv.CSVWriter;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports optimization results and allocation data to CSV files.
 */
public class CsvExportService {

    private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports a single optimization result (header + all route allocations) to a CSV file.
     *
     * @param result    result to export
     * @param filePath  target file path (e.g. /home/user/run_42.csv)
     */
    public void exportResult(OptimizationResult result, String filePath) throws IOException {
        log.info("Exporting result id={} to {}", result.getId(), filePath);
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            // ── Summary header block ──────────────────────────────────────────
            writer.writeNext(new String[]{"PDS LOGISTICS OPTIMIZATION REPORT"});
            writer.writeNext(new String[]{"Scenario",         result.getScenarioName()});
            writer.writeNext(new String[]{"Grain Type",       result.getGrainType().getDisplayName()});
            writer.writeNext(new String[]{"Run Timestamp",    result.getRunTimestamp() != null
                    ? result.getRunTimestamp().format(DT_FMT) : "N/A"});
            writer.writeNext(new String[]{"Algorithm",        result.getAlgorithmUsed()});
            writer.writeNext(new String[]{"Status",           result.getStatus()});
            writer.writeNext(new String[]{"Total Supply (MT)", "%.2f".formatted(result.getTotalSupplyMT())});
            writer.writeNext(new String[]{"Total Demand (MT)", "%.2f".formatted(result.getTotalDemandMT())});
            writer.writeNext(new String[]{"Transported (MT)", "%.2f".formatted(result.getTotalTransportedMT())});
            writer.writeNext(new String[]{"Total Cost (INR)", "%.2f".formatted(result.getTotalCost())});
            writer.writeNext(new String[]{"Fulfilled Demand (MT)", "%.2f".formatted(result.getFulfilledDemandMT())});
            writer.writeNext(new String[]{"Unmet Demand (MT)", "%.2f".formatted(result.getUnmetDemandMT())});
            writer.writeNext(new String[]{"Network Efficiency (%)", "%.2f".formatted(result.getNetworkEfficiencyPct())});
            writer.writeNext(new String[]{"Solve Duration (ms)", String.valueOf(result.getSolveDurationMs())});
            writer.writeNext(new String[]{}); // blank line

            // ── Allocation table ─────────────────────────────────────────────
            writer.writeNext(new String[]{
                    "Route ID", "Source Code", "Source Name",
                    "Destination Code", "Destination Name",
                    "Mode", "Distance (km)", "Capacity (MT)",
                    "Allocated (MT)", "Unit Cost (₹/MT)", "Cost Contribution (₹)"
            });

            for (AllocationResult alloc : result.getAllocations()) {
                TransportRoute route = alloc.getRoute();
                writer.writeNext(new String[]{
                        String.valueOf(route.getId()),
                        route.getSource().getCode(),
                        route.getSource().getName(),
                        route.getDestination().getCode(),
                        route.getDestination().getName(),
                        route.getTransportMode(),
                        "%.1f".formatted(route.getDistanceKm()),
                        "%.1f".formatted(route.getCapacityMT()),
                        "%.2f".formatted(alloc.getAllocatedMT()),
                        "%.2f".formatted(route.getUnitCostPerMT()),
                        "%.2f".formatted(alloc.getCostContribution())
                });
            }

            log.info("CSV export complete: {} allocation rows", result.getAllocations().size());
        }
    }

    /**
     * Exports the history list (summary rows, no allocations) to CSV.
     */
    public void exportHistory(List<OptimizationResult> history, String filePath) throws IOException {
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{
                    "Run ID", "Scenario", "Grain", "Timestamp", "Status",
                    "Transported (MT)", "Total Cost (₹)", "Efficiency (%)", "Duration (ms)"
            });

            for (OptimizationResult r : history) {
                writer.writeNext(new String[]{
                        String.valueOf(r.getId()),
                        r.getScenarioName(),
                        r.getGrainType().getDisplayName(),
                        r.getRunTimestamp() != null ? r.getRunTimestamp().format(DT_FMT) : "",
                        r.getStatus(),
                        "%.2f".formatted(r.getTotalTransportedMT()),
                        "%.2f".formatted(r.getTotalCost()),
                        "%.2f".formatted(r.getNetworkEfficiencyPct()),
                        String.valueOf(r.getSolveDurationMs())
                });
            }
        }
    }
}
