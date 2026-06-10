package test;

import model.Employee;
import repository.EmployeeRepository;

public class EmployeeRepositoryTest {

    public static void main(String[] args) {

        EmployeeRepository repo = new EmployeeRepository();

        System.out.println("===== CREATE =====");

        Employee emp = new Employee();
        emp.setId("EMP9999");
        emp.setFullName("Test User");

        repo.add(emp);

        System.out.println(repo.findById("EMP9999"));

        System.out.println("===== UPDATE =====");

        emp.setFullName("Updated User");

        repo.update(emp);

        System.out.println(repo.findById("EMP9999"));

        System.out.println("===== DELETE =====");

        repo.delete("EMP9999");

        System.out.println(repo.findById("EMP9999"));

        System.out.println("===== COUNT =====");
        System.out.println(repo.count());
    }
}