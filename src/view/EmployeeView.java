package view;

import controller.EmployeeController;
import model.Employee;
import model.EmpType;
import model.EmployeeStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class EmployeeView {

    private final EmployeeController controller = new EmployeeController();
    private final Scanner scanner = new Scanner(System.in);

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== EMPLOYEE MENU =====");
            System.out.println("1. Them nhan vien");
            System.out.println("2. Tim nhan vien theo ma");
            System.out.println("3. Tim nhan vien theo phong ban");
            System.out.println("4. Cap nhat nhan vien");
            System.out.println("5. Xoa nhan vien");
            System.out.println("0. Thoat");
            System.out.print("Chon: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": addEmployee(); break;
                case "2": findById(); break;
                case "3": findByDept(); break;
                case "4": updateEmployee(); break;
                case "5": deleteEmployee(); break;
                case "0": running = false; break;
                default: System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void addEmployee() {
        try {
            System.out.print("Ma nhan vien: ");
            String id = scanner.nextLine();
            System.out.print("Ho ten: ");
            String name = scanner.nextLine();
            System.out.print("Ma phong ban: ");
            String deptId = scanner.nextLine();
            System.out.print("Loai (FULLTIME/PARTTIME): ");
            EmpType type = EmpType.FULLTIME.fromString(scanner.nextLine());
            System.out.print("Luong co ban: ");
            double salary = Double.parseDouble(scanner.nextLine());
            System.out.print("Thue suat (0-1): ");
            double taxRate = Double.parseDouble(scanner.nextLine());
            System.out.print("Ngay vao lam (yyyy-MM-dd): ");
            LocalDate joinDate = LocalDate.parse(scanner.nextLine());

            Employee emp = new Employee(id, name, deptId, type,
                    salary, taxRate, joinDate, EmployeeStatus.ACTIVE);

            controller.add(emp);
            System.out.println("Them nhan vien thanh cong!");
        } catch (Exception e) {
            System.out.println("Loi: " + e.getMessage());
        }
    }

    private void findById() {
        System.out.print("Nhap ma nhan vien: ");
        String id = scanner.nextLine();
        Employee emp = controller.findById(id);
        System.out.println(emp != null ? emp : "Khong tim thay nhan vien.");
    }

    private void findByDept() {
        System.out.print("Nhap ma phong ban: ");
        String deptId = scanner.nextLine();
        List<Employee> list = controller.findByDept(deptId);
        if (list.isEmpty()) {
            System.out.println("Khong co nhan vien nao trong phong ban nay.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private void updateEmployee() {
        try {
            System.out.print("Nhap ma nhan vien can sua: ");
            String id = scanner.nextLine();
            Employee emp = controller.findById(id);

            if (emp == null) {
                System.out.println("Khong tim thay nhan vien.");
                return;
            }

            System.out.print("Ho ten moi (" + emp.getFullName() + "): ");
            String name = scanner.nextLine();
            if (!name.isEmpty()) emp.setFullName(name);

            System.out.print("Luong co ban moi (" + emp.getBaseSalary() + "): ");
            String salaryStr = scanner.nextLine();
            if (!salaryStr.isEmpty()) emp.setBaseSalary(Double.parseDouble(salaryStr));

            boolean success = controller.update(emp);
            System.out.println(success ? "Cap nhat thanh cong!" : "Cap nhat khong thanh cong.");
        } catch (Exception e) {
            System.out.println("Loi: " + e.getMessage());
        }
    }

    private void deleteEmployee() {
        System.out.print("Nhap ma nhan vien can xoa: ");
        String id = scanner.nextLine();
        boolean success = controller.delete(id);
        System.out.println(success ? "Xoa thanh cong!" : "Khong tim thay nhan vien de xoa.");
    }
}
    

