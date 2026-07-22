package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import simulation.LockMechanism;
import simulation.PayrollSimulationService;
import simulation.SimulationDetector;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollSimulationServiceTest {
    @Test
    void noLockProducesDuplicatesWhileLockingMechanismsDoNot(@TempDir Path temp) throws Exception {
        Path source = temp.resolve("source.csv");
        String header = "entryId,empId,deptId,month,year,baseSalary,overtimePay,absenceDeduction,bonus,taxAmount,netSalary,status,version,processedAt\n";
        String row = "PE1,E1,D1,12,2025,1000,0,0,0,100,900,PENDING,0,\n";
        Files.writeString(source, header + row);
        PayrollSimulationService service = new PayrollSimulationService();
        SimulationDetector detector = new SimulationDetector();

        Path noLock = temp.resolve("no_lock.csv");
        service.run(20, 1, 1, LockMechanism.NO_LOCK, source.toString(), noLock.toString());
        assertTrue(detector.detectDoublePayments(noLock.toString()) > 0);

        for (LockMechanism mechanism : new LockMechanism[]{LockMechanism.SYNCHRONIZED,
                LockMechanism.OPTIMISTIC, LockMechanism.FILE_LOCK}) {
            Path output = temp.resolve(mechanism + ".csv");
            int success = service.run(20, 1, 1, mechanism, source.toString(), output.toString());
            assertEquals(1, success, mechanism.name() + " must process every unique entry");
            assertEquals(0, detector.detectDoublePayments(output.toString()), mechanism.name());
        }
    }
}
