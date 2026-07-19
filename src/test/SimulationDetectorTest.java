package test;

import model.LeaveBalance;
import model.LeaveRequest;
import model.LeaveStatus;
import model.LeaveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import simulation.SimulationDetector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationDetectorTest {
    private final SimulationDetector detector = new SimulationDetector();

    @Test
    void detectsDuplicateEntryIdsFromCsv(@TempDir Path temp) throws Exception {
        Path csv = temp.resolve("result.csv");
        Files.writeString(csv, "entryId,empId\nPE1,E1\nPE1,E1\nPE2,E2\nPE2,E2\nPE2,E2\n");
        assertEquals(3, detector.detectDoublePayments(csv.toString()));
    }

    @Test
    void detectsLeaveBalanceDifferentFromApprovedRequests() {
        LeaveBalance balance = new LeaveBalance();
        balance.setId("LB1");
        balance.setEmpId("E1");
        balance.setYear(2025);
        balance.setAnnualTotal(12);
        balance.setAnnualUsed(5);
        balance.setSickTotal(6);
        balance.setSickUsed(0);

        LeaveRequest request = new LeaveRequest();
        request.setId("LR1");
        request.setEmpId("E1");
        request.setType(LeaveType.ANNUAL);
        request.setDays(2);
        request.setStatus(LeaveStatus.APPROVED);

        assertEquals(1, detector.detectWrongLeaveDeductions(
                List.of(balance), List.of(request)));
    }
}
