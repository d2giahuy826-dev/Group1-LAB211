package repository;

import model.Department;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentRepository {
    private final String FILE_PATH = "data/departments.csv";

    public DepartmentRepository() {
        ensureFileExists();
    }

    private void ensureFileExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating department data file: " + e.getMessage());
            }
        }
    }

    public List<Department> getAll() {
        List<Department> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Department dept = new Department();
                dept.fromCsvLine(line); // Đọc chuỗi CSV 5 trường có location
                list.add(dept);
            }
        } catch (IOException e) {
            System.err.println("Error reading departments: " + e.getMessage());
        }
        return list;
    }

    public void saveAll(List<Department> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Department dept : list) {
                pw.println(dept.toCsvLine()); // Xuất chuỗi CSV kết hợp location
            }
        } catch (IOException e) {
            System.err.println("Error saving departments: " + e.getMessage());
        }
    }

    public void add(Department dept) {
        List<Department> list = getAll();
        list.add(dept);
        saveAll(list);
    }

    public Department findById(String id) {
        return getAll().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}