package controller;

import model.Employee;
import repository.EmployeeRepository;

import java.util.List;
import java.util.Optional;

public class EmployeeController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

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

    public void add(Employee employee) {
        employeeRepository.add(employee);
    }

    public boolean update(Employee employee) {
        return employeeRepository.update(employee);
    }

    public boolean delete(String empId) {
        return employeeRepository.delete(empId);
    }
}
    

