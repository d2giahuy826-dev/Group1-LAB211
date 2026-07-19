package repository;

import model.AttendanceRecord;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import util.ProjectPath;

public class AttendanceRepository {
    private final String filePath;

    public AttendanceRepository() {
        this.filePath = ProjectPath.resolve("data/attendance.csv").toString();
        ensureFileExists();
    }

    private void ensureFileExists() {
        File file = new File(filePath);
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
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isHeaderLine(line, "attendId")) continue;
                AttendanceRecord record = new AttendanceRecord();
                record.fromCsvLine(line);
                list.add(record);
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance: " + e.getMessage());
        }
        return list;
    }

    private boolean isHeaderLine(String line, String firstColumnName) {
        String[] parts = line.split(",", -1);
        return parts.length > 0 && parts[0].trim().equalsIgnoreCase(firstColumnName);
    }

    public void saveAll(List<AttendanceRecord> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
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
                .filter(r -> r.getEmpId().equals(employeeId))
                .collect(Collectors.toList());
    }
}
