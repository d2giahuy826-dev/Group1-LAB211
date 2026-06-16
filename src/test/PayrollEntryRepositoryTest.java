package test;

import exception.DuplicatePaymentException;
import model.PayStatus;
import model.PayrollEntry;
import org.junit.jupiter.api.Test;
import repository.PayrollEntryRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class PayrollEntryRepositoryTest {

    @Test
    public void testProcessWithSyncPreventsDoublePaymentWithTwoThreads()
            throws IOException, InterruptedException {

        String path = "data/test_payroll_entries.csv";
        File file = new File(path);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("entryId,empId,deptId,month,year,baseSalary,overtimePay,absenceDeduction,bonus,taxAmount,netSalary,status,version,processedAt\n");
            writer.write("PE001,EMP001,DEP001,1,2024,10000000,0,0,0,1000000,9000000,PENDING,0,\n");
        }

        PayrollEntryRepository repo = new PayrollEntryRepository(path);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                repo.processWithSync("PE001");
                successCount.incrementAndGet();
            } catch (DuplicatePaymentException e) {
                duplicateCount.incrementAndGet();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertEquals(1, successCount.get());
        assertEquals(1, duplicateCount.get());

        PayrollEntry result = repo.findById("PE001");

        assertNotNull(result);
        assertEquals(PayStatus.PROCESSED, result.getStatus());

        file.delete();
    }
}