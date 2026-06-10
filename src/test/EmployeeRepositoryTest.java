package test;

import model.*;
import repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.Scanner;

public class EmployeeRepositoryTest {


public static void main(String[] args) {

    Scanner sc = new Scanner(System.in);
    EmployeeRepository repo = new EmployeeRepository();

    // ===== CREATE =====
    System.out.println("===== CREATE EMPLOYEE =====");

    System.out.print("Employee ID: ");
    String id = sc.nextLine();

    System.out.print("Full Name: ");
    String name = sc.nextLine();

    System.out.print("Department ID: ");
    String deptId = sc.nextLine();

    System.out.print("Base Salary: ");
    double salary = Double.parseDouble(sc.nextLine());

    Employee emp = new Employee(
            id,
            name,
            deptId,
            EmpType.FULLTIME,
            salary,
            0.1,
            LocalDate.now(),
            EmployeeStatus.ACTIVE
    );

    repo.add(emp);

    System.out.println("\nEmployee added successfully!");
    System.out.println(repo.findById(id));

    // ===== UPDATE =====
    System.out.println("\n===== UPDATE EMPLOYEE =====");

    System.out.print("New Full Name: ");
    String newName = sc.nextLine();

    emp.setFullName(newName);

    if (repo.update(emp)) {
        System.out.println("Update successful!");
    }

    System.out.println(repo.findById(id));

    // ===== DELETE =====
    System.out.println("\n===== DELETE EMPLOYEE =====");

    System.out.print("Delete this employee? (Y/N): ");
    String confirm = sc.nextLine();

    if (confirm.equalsIgnoreCase("Y")) {

        if (repo.delete(id)) {
            System.out.println("Delete successful!");
        }

        System.out.println(repo.findById(id));
    }

    // ===== COUNT =====
    System.out.println("\n===== TOTAL RECORDS =====");
    System.out.println(repo.count());

    sc.close();
}


}
