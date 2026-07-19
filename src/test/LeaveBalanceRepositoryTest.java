package test;

import model.LeaveBalance;
import repository.LeaveBalanceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

public class LeaveBalanceRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void testAddAndFind() throws Exception {

        Path file = tempDir.resolve("balance-add.csv");
        Files.writeString(file, "balanceId,empId,year,annualTotal,annualUsed,annualRemaining,sickTotal,sickUsed,sickRemaining,version\n");

        LeaveBalanceRepository repo =
                new LeaveBalanceRepository(file.toString());

        LeaveBalance balance =
                new LeaveBalance(
                        "LB001",
                        "EMP001",
                        2025,
                        12,
                        2,
                        6,
                        1,
                        0
                );

        repo.add(balance);

        LeaveBalance result =
                repo.findByEmployeeId("EMP001");

        assertNotNull(result);
        assertEquals("EMP001", result.getEmpId());
    }

    @Test
    void testGetAll() throws Exception {

        Path file = tempDir.resolve("balance-all.csv");
        Files.writeString(file, "balanceId,empId,year,annualTotal,annualUsed,annualRemaining,sickTotal,sickUsed,sickRemaining,version\n");

        LeaveBalanceRepository repo =
                new LeaveBalanceRepository(file.toString());

        repo.add(new LeaveBalance(
                "LB001","EMP001",
                2025,12,2,
                6,1,0));

        repo.add(new LeaveBalance(
                "LB002","EMP002",
                2025,12,0,
                6,0,0));

        assertEquals(2, repo.getAll().size());
    }
}
