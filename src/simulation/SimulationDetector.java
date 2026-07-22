package simulation;

import model.LeaveBalance;
import model.LeaveRequest;
import model.LeaveStatus;
import model.LeaveType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Post-simulation checks. Every check reloads CSV data instead of using RAM counters. */
public class SimulationDetector {

    public int detectDoublePayments(String csvPath) {
        Map<String, Integer> counts = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(csvPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.toLowerCase().startsWith("entryid,")) continue;
                String id = line.split(",", -1)[0].trim();
                counts.merge(id, 1, Integer::sum);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot inspect simulation CSV: " + csvPath, e);
        }
        return counts.values().stream().mapToInt(n -> Math.max(0, n - 1)).sum();
    }

    public int detectWrongLeaveDeductions(List<LeaveBalance> balances,
                                          List<LeaveRequest> requests) {
        Map<String, Integer> approved = new HashMap<>();
        for (LeaveRequest request : requests) {
            if (request.getStatus() == LeaveStatus.APPROVED) {
                String key = request.getEmpId() + "|" + request.getType();
                approved.merge(key, request.getDays(), Integer::sum);
            }
        }
        int errors = 0;
        for (LeaveBalance balance : balances) {
            int annual = approved.getOrDefault(balance.getEmpId() + "|" + LeaveType.ANNUAL, 0);
            int sick = approved.getOrDefault(balance.getEmpId() + "|" + LeaveType.SICK, 0);
            if (balance.getAnnualUsed() != annual) errors++;
            if (balance.getSickUsed() != sick) errors++;
        }
        return errors;
    }
}
