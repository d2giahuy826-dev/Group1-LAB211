package repository;

import model.AttendanceRecord;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttendanceRepository {
    private final String FILE_PATH = "data/attendance.csv";

    public AttendanceRepository() {
        ensureFileExists();
    }

    private void ensureFileExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating attendance data file: " + e.getMessage());
            }
        }
    }

    public List<AttendanceRecord> getAll() {
        List<AttendanceRecord> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                AttendanceRecord record = new AttendanceRecord();
                record.fromCsvLine(line);
                list.add(record);
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance: " + e.getMessage());
        }
        return list;
    }

    public void saveAll(List<AttendanceRecord> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (AttendanceRecord record : list) {
                pw.println(record.toCsvLine());
            }
        } catch (IOException e) {
            System.err.println("Error saving attendance: " + e.getMessage());
        }
    }

    public void add(AttendanceRecord record) {
        List<AttendanceRecord> list = getAll();
        list.add(record);
        saveAll(list);
    }

    public List<AttendanceRecord> findByEmployeeId(String employeeId) {
        return getAll().stream()
                .filter(r -> r.getEmployeeId().equals(employeeId))
                .collect(Collectors.toList());
    }
}