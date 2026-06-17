package test;

import model.LeaveBalance;
import repository.LeaveBalanceRepository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LeaveBalanceRepositoryTest {

    @Test
    void testAddAndFind() {

        LeaveBalanceRepository repo =
                new LeaveBalanceRepository();

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
    void testGetAll() {

        LeaveBalanceRepository repo =
                new LeaveBalanceRepository();

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