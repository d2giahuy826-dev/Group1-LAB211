package view;

import controller.EmployeeController;
import model.Employee;
import model.EmpType;
import model.EmployeeStatus;
import java.time.LocalDate;
import java.util.List;

public class EmployeeView {

    private final EmployeeController controller;
    private final MainView main;

    // ─── Constructor (Dependency Injection từ MainView) ───────────────────────
    public EmployeeView(EmployeeController controller, MainView main) {
        this.controller = controller;
        this.main       = main;
    }

    // ─── Entry point ──────────────────────────────────────────────────────────
    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = main.readInt("Chon: ");

            switch (choice) {
                case 1: addEmployee();    break;
                case 2: findById();       break;
                case 3: findByDept();     break;
                case 4: updateEmployee(); break;
                case 5: deleteEmployee(); break;
                case 6: viewAll();        break;
                case 0: running = false;  break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────
    private void printMenu() {
        System.out.println("\n┌──────────────────────────────────────────┐");
        System.out.println(  "│          QUAN LY NHAN VIEN               │");
        System.out.println(  "├──────────────────────────────────────────┤");
        System.out.println(  "│  1. Them nhan vien                       │");
        System.out.println(  "│  2. Tim nhan vien theo ma                │");
        System.out.println(  "│  3. Tim nhan vien theo phong ban          │");
        System.out.println(  "│  4. Cap nhat nhan vien                   │");
        System.out.println(  "│  5. Xoa nhan vien                        │");
        System.out.println(  "│  6. Xem tat ca nhan vien                 │");
        System.out.println(  "│  0. Quay lai Menu chinh                  │");
        System.out.println(  "└──────────────────────────────────────────┘");
    }

    // ─── Actions ──────────────────────────────────────────────────────────────
    private void addEmployee() {
        System.out.println("\n  === THEM NHAN VIEN ===");
        try {
            String id      = main.readString("  Ma nhan vien: ");
            String name    = main.readString("  Ho ten: ");
            String deptId  = main.readString("  Ma phong ban: ");

            System.out.print("  Loai (FULLTIME/PARTTIME): ");
            EmpType type   = EmpType.FULLTIME.fromString(main.getScanner().nextLine().trim());

            double salary  = Double.parseDouble(main.readString("  Luong co ban: "));
            double taxRate = Double.parseDouble(main.readString("  Thue suat (0-1): "));

            String dateStr = main.readString("  Ngay vao lam (yyyy-MM-dd): ");
            LocalDate joinDate = LocalDate.parse(dateStr);

            Employee emp = new Employee(id, name, deptId, type,
                    salary, taxRate, joinDate, EmployeeStatus.ACTIVE);

            controller.add(emp);
            System.out.println("  ✓ Them nhan vien thanh cong!");
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void findById() {
        System.out.println("\n  === TIM NHAN VIEN THEO MA ===");
        String id  = main.readString("  Nhap ma nhan vien: ");
        Employee emp = controller.findById(id);
        if (emp != null) {
            printEmployee(emp);
        } else {
            System.out.println("  [!] Khong tim thay nhan vien.");
        }
    }

    private void findByDept() {
        System.out.println("\n  === TIM NHAN VIEN THEO PHONG BAN ===");
        String deptId       = main.readString("  Nhap ma phong ban: ");
        List<Employee> list = controller.findByDept(deptId);
        if (list.isEmpty()) {
            System.out.println("  [!] Khong co nhan vien nao trong phong ban nay.");
        } else {
            System.out.printf("  Tim thay %d nhan vien:%n", list.size());
            list.forEach(this::printEmployee);
        }
    }

    private void updateEmployee() {
        System.out.println("\n  === CAP NHAT NHAN VIEN ===");
        try {
            String id    = main.readString("  Nhap ma nhan vien can sua: ");
            Employee emp = controller.findById(id);

            if (emp == null) {
                System.out.println("  [!] Khong tim thay nhan vien.");
                return;
            }

            System.out.printf("  Ho ten hien tai: %s%n", emp.getFullName());
            String name = main.readOptionalString("  Ho ten moi (Enter de giu nguyen): ");
            if (!name.isEmpty()) emp.setFullName(name);

            System.out.printf("  Luong co ban hien tai: %,.0f%n", emp.getBaseSalary());
            String salaryStr = main.readOptionalString("  Luong co ban moi (Enter de giu nguyen): ");
            if (!salaryStr.isEmpty()) emp.setBaseSalary(Double.parseDouble(salaryStr));

            boolean success = controller.update(emp);
            System.out.println(success
                    ? "  ✓ Cap nhat thanh cong!"
                    : "  [!] Cap nhat khong thanh cong.");
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void deleteEmployee() {
        System.out.println("\n  === XOA NHAN VIEN ===");
        String id      = main.readString("  Nhap ma nhan vien can xoa: ");
        boolean success = controller.delete(id);
        System.out.println(success
                ? "  ✓ Xoa thanh cong!"
                : "  [!] Khong tim thay nhan vien de xoa.");
    }

    private void viewAll() {
        System.out.println("\n  === TAT CA NHAN VIEN ===");
        List<Employee> list = controller.getAll();
        if (list.isEmpty()) {
            System.out.println("  Chua co nhan vien nao.");
        } else {
            System.out.printf("  Tong so nhan vien: %d%n", list.size());
            list.forEach(this::printEmployee);
        }
    }

    // ─── Print helper ─────────────────────────────────────────────────────────
    private void printEmployee(Employee emp) {
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf( "  │  Ma NV     : %-27s│%n", emp.getId());
        System.out.printf( "  │  Ho ten    : %-27s│%n", emp.getFullName());
        System.out.printf( "  │  Phong ban : %-27s│%n", emp.getDeptId());
        System.out.printf( "  │  Loai      : %-27s│%n", emp.getEmpType());
        System.out.printf( "  │  Luong CB  : %,27.0f │%n", emp.getBaseSalary());
        System.out.printf( "  │  Trang thai: %-27s│%n", emp.getStatus());
        System.out.println("  └─────────────────────────────────────────┘");
    }
}