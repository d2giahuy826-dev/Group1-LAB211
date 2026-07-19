package simulation;

import controller.SimulatorController;
import repository.LeaveBalanceRepository;
import repository.LeaveRequestRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import util.ConsoleTable;
import util.ProjectPath;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Task 9 reproducible experiment: 20 threads x 4 mechanisms x 3 runs. */
public class ExperimentRunner {
    public static void main(String[] args) throws IOException {
        Path outputDir = ProjectPath.resolve(args.length == 0 ? "docs/experiment" : args[0]);
        Files.createDirectories(outputDir);
        String sessionId = "run-" + ProcessHandle.current().pid() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        Path rawOutputDir = outputDir.resolve("raw").resolve(sessionId);
        Files.createDirectories(rawOutputDir);
        SimulatorController controller = new SimulatorController(
                new PayrollSimulationService(), new SimulationDetector(),
                new LeaveBalanceRepository(), new LeaveRequestRepository(),
                "data/payroll_entries.csv", rawOutputDir.toString());

        List<Row> rows = new ArrayList<>();
        printTitle("FULL EXPERIMENT - 20 THREADS x 4 MECHANISMS x 3 RUNS");
        new ConsoleTable("THREADS", "DEPARTMENTS", "EMP / DEPT", "TOTAL EMP", "REPEAT")
                .addRow(20, 20, 50, 1000, 3).print();
        System.out.println();
        for (LockMechanism mechanism : LockMechanism.values()) {
            for (int run = 1; run <= 3; run++) {
                System.out.printf("[RUN %02d/12] %-13s - attempt %d ... ", rows.size() + 1, mechanism, run);
                SimulationResult result = controller.runSimulation(20, 20, 50, mechanism);
                rows.add(new Row(mechanism, run, result));
                System.out.printf("DONE (%d ms)%n", result.elapsedMs);
            }
        }
        writeCsv(outputDir.resolve("experiment_results.csv"), rows);
        writeJson(outputDir.resolve("experiment_results.json"), rows);
        printDetailedTable(rows);
        printAverageTable(rows);
        System.out.println("CSV : " + outputDir.resolve("experiment_results.csv").toAbsolutePath());
        System.out.println("JSON: " + outputDir.resolve("experiment_results.json").toAbsolutePath());
        System.out.println("RAW : " + rawOutputDir.toAbsolutePath());
    }

    private static void printDetailedTable(List<Row> rows) {
        System.out.println();
        printTitle("DETAILED RESULTS");
        ConsoleTable table = new ConsoleTable(
                "MECHANISM", "RUN", "SUCCESS", "DUPLICATE", "WRONG LEAVE", "TIME (ms)", "TPS");
        for (Row row : rows) table.addRow(row.mechanism, row.run, row.result.success,
                row.result.doublePayment, row.result.wrongLeave, row.result.elapsedMs,
                String.format("%.2f", row.result.tps));
        table.print();
    }

    private static void printAverageTable(List<Row> rows) {
        System.out.println();
        printTitle("AVERAGE OF 3 RUNS");
        ConsoleTable table = new ConsoleTable(
                "MECHANISM", "AVG DUPLICATE", "AVG WRONG LEAVE", "AVG TIME (ms)", "AVG TPS");
        for (LockMechanism mechanism : LockMechanism.values()) {
            List<Row> group = rows.stream().filter(row -> row.mechanism == mechanism).toList();
            double duplicate = group.stream().mapToInt(row -> row.result.doublePayment).average().orElse(0);
            double leave = group.stream().mapToInt(row -> row.result.wrongLeave).average().orElse(0);
            double time = group.stream().mapToLong(row -> row.result.elapsedMs).average().orElse(0);
            double tps = group.stream().mapToDouble(row -> row.result.tps).average().orElse(0);
            table.addRow(mechanism, String.format("%.2f", duplicate), String.format("%.2f", leave),
                    String.format("%.2f", time), String.format("%.2f", tps));
        }
        table.print();
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(92));
        System.out.println(title);
        System.out.println("=".repeat(92));
    }

    private static void writeCsv(Path path, List<Row> rows) throws IOException {
        StringBuilder out = new StringBuilder("mechanism,run,threads,departments,employees,success,doublePayment,wrongLeave,elapsedMs,TPS\n");
        for (Row row : rows) out.append(row.csv()).append('\n');
        Files.writeString(path, out, StandardCharsets.UTF_8);
    }

    private static void writeJson(Path path, List<Row> rows) throws IOException {
        StringBuilder out = new StringBuilder("[\n");
        for (int i = 0; i < rows.size(); i++) {
            out.append("  ").append(rows.get(i).json());
            out.append(i + 1 == rows.size() ? "\n" : ",\n");
        }
        Files.writeString(path, out.append("]\n"), StandardCharsets.UTF_8);
    }

    private static class Row {
        final LockMechanism mechanism; final int run; final SimulationResult result;
        Row(LockMechanism mechanism, int run, SimulationResult result) {
            this.mechanism = mechanism; this.run = run; this.result = result;
        }
        String csv() { return String.format(java.util.Locale.US,
                "%s,%d,20,20,1000,%d,%d,%d,%d,%.4f", mechanism, run,
                result.success, result.doublePayment, result.wrongLeave, result.elapsedMs, result.tps); }
        String json() { return String.format(java.util.Locale.US,
                "{\"mechanism\":\"%s\",\"run\":%d,\"threads\":20,\"departments\":20,\"employees\":1000,\"success\":%d,\"doublePayment\":%d,\"wrongLeave\":%d,\"elapsedMs\":%d,\"TPS\":%.4f}",
                mechanism, run, result.success, result.doublePayment, result.wrongLeave,
                result.elapsedMs, result.tps); }
    }
}
