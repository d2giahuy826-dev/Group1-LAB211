package controller;

import model.Department;
import repository.DepartmentRepository;

import java.util.List;

/**
 * DepartmentController – Điều phối quản lý phòng ban.
 *
 * Use case: Assign Manager, View Department Report (HR Staff).
 */
public class DepartmentController {

    private final DepartmentRepository departmentRepository = new DepartmentRepository();

    public List<Department> getAll() {
        return departmentRepository.getAll();
    }

    public Department findById(String deptId) {
        return departmentRepository.findById(deptId);
    }

    /**
     * Gán quản lý (manager) cho một phòng ban.
     *
     * @return true nếu gán thành công, false nếu không tìm thấy phòng ban
     */
    public boolean assignManager(String deptId, String managerId) {
        Department dept = departmentRepository.findById(deptId);
        if (dept == null) {
            return false;
        }
        dept.setManagerId(managerId);
        return departmentRepository.update(dept);
    }
}
