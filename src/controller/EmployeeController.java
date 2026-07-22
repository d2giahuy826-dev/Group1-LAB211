package controller;

import model.Employee;
import model.User;
import model.UserRole;
import repository.DepartmentRepository;
import repository.EmployeeRepository;
import repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmployeeController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final UserRepository userRepository = new UserRepository();
    private final DepartmentRepository departmentRepository = new DepartmentRepository();

    public Employee findById(String empId) {
        Optional<Employee> result = employeeRepository.findById(empId);
        return result.orElse(null);
    }

    public List<Employee> findByDept(String deptId) {
        return employeeRepository.findByField("deptId", deptId);
    }

    public List<Employee> getAll() {
        return employeeRepository.loadAll();
    }

    public void addEmployeeWithAccount(Employee employee, String username, String password) {
        if (employee.getId() == null || !employee.getId().matches("^EMP\\d+$")) {
            throw new IllegalArgumentException(
                "Ma nhan vien phai bat dau bang 'EMP' va theo sau la cac chu so (vi du: EMP1010).");
        }
        if (departmentRepository.findById(employee.getDeptId()) == null) {
            throw new IllegalArgumentException(
                "Ma phong ban '" + employee.getDeptId() + "' khong ton tai.");
        }
        if (employee.getJoinDate() != null && employee.getJoinDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Ngay vao lam khong duoc o trong tuong lai.");
        }
        employeeRepository.add(employee);
        try {
            User user = new User(employee.getId(), username, password, UserRole.EMPLOYEE);
            userRepository.add(user);
        } catch (RuntimeException e) {
            employeeRepository.delete(employee.getId());
            throw e;
        }
    }

    /**
     * Cap nhat nhan vien. Se validate deptId (neu co thay doi) phai ton tai
     * that trong departments.csv, tranh chuyen nhan vien sang phong ban "ao".
     */
    public boolean update(Employee employee) {
        if (departmentRepository.findById(employee.getDeptId()) == null) {
            throw new IllegalArgumentException(
                "Ma phong ban '" + employee.getDeptId() + "' khong ton tai.");
        }
        return employeeRepository.update(employee);
    }

    /**
     * Neu khong xoa User thi nhan vien da bi xoa van dang nhap duoc vao he thong.
     */
    public boolean delete(String empId) {
        boolean employeeDeleted = employeeRepository.delete(empId);
        if (employeeDeleted) {
            userRepository.delete(empId); // userId == empId (quy uoc cho role EMPLOYEE)
        }
        return employeeDeleted;
    }
}