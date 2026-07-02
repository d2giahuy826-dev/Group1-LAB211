package controller;

import model.Employee;
import model.User;
import model.UserRole;
import repository.EmployeeRepository;
import repository.UserRepository;
import java.util.List;
import java.util.Optional;

public class EmployeeController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final UserRepository userRepository = new UserRepository();

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
    employeeRepository.add(employee);
    try {
        User user = new User(employee.getId(), username, password, UserRole.EMPLOYEE);
        userRepository.add(user);
    } catch (RuntimeException e) {
        employeeRepository.delete(employee.getId());
        throw e;
    }
}
    public boolean update(Employee employee) {
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
    

