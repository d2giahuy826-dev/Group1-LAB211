package repository;

import model.Employee;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {
    private final String FILE_PATH = "data/employees.csv";

    public EmployeeRepository() {
        ensureFileExists();
    }

    private void ensureFileExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating employee data file: " + e.getMessage());
            }
        }
    }

    public List<Employee> getAll() {
        List<Employee> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isHeaderLine(line, "empId")) continue;

                try {
                    Employee employee = new Employee();
                    employee.fromCsvLine(line);
                    list.add(employee);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: skip invalid employee CSV row: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading employees: " + e.getMessage());
        }
        return list;
    }

    private boolean isHeaderLine(String line, String firstColumnName) {
        String[] parts = line.split(",", -1);
        return parts.length > 0 && parts[0].trim().equalsIgnoreCase(firstColumnName);
    }

    public void saveAll(List<Employee> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Employee employee : list) {
                pw.println(employee.toCsvLine());
            }
        } catch (IOException e) {
            System.err.println("Error saving employees: " + e.getMessage());
        }
    }

    public void add(Employee employee) {
        List<Employee> list = getAll();
        list.add(employee);
        saveAll(list);
    }

    public Employee findById(String id) {
        return getAll().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}