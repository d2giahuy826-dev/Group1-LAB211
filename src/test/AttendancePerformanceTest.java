package test;

import repository.AttendanceRepository;
import model.AttendanceRecord;

import java.util.List;

public class AttendancePerformanceTest {

    public static void main(String[] args) {

        AttendanceRepository repo = new AttendanceRepository();

        long start = System.nanoTime();

        List<AttendanceRecord> records = repo.getAll();

        long end = System.nanoTime();

        double timeMs = (end - start) / 1_000_000.0;

        System.out.println("Records loaded: " + records.size());
        System.out.printf("Execution time: %.3f ms%n", timeMs);

        if (timeMs < 1000) {
            System.out.println("PASS (< 1 second)");
        } else {
            System.out.println("FAIL (> 1 second)");
        }
    }
}