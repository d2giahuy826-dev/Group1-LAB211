package view;

import controller.AttendanceController;
import controller.AuthController;
import controller.DepartmentController;
import controller.EmployeeController;
import controller.LeaveController;
import controller.PayrollController;
import model.User;
import model.UserRole;

import java.util.Scanner;

/**
 * MainView – Menu console chính của hệ thống Payroll.
 *
 * Sau khi Login, mỗi Role (Employee / HR Staff / Payroll Staff) chỉ thấy
 * đúng các chức năng (use case) của mình theo sơ đồ Use Case:
 *
 *   EMPLOYEE:
 *     - Leave Management   : Submit Leave Request, View Leave Balance
 *     - Attendance         : Check in, Check out, View Attendance Record,
 *                             Submit Attendance Adjustment Request
 *     - Payroll            : Xem luong cua chinh minh
 *
 *   HR_STAFF:
 *     - Employee Management: Add/Update/Search/Delete/View All Employees
 *     - Department Mgmt    : Assign Manager, View Department Report
 *     - Leave Management   : Approve/Reject Leave Request
 *     - Attendance Mgmt    : View Attendance Summary, List Attendance Records,
 *                             Review/Approve/Reject Attendance Adjustment Request
 *     - Reporting          : Generate Department/Attendance Report
 *
 *   PAYROLL_STAFF:
 *     - Payroll Processing : Run Monthly Payroll, View Payroll By Period,
 *                             View Payroll History, Conduct Payroll Audit
 *     - Reporting & Data   : Generate Payroll Report, Export Payroll CSV
 *     - Attendance         : List Attendance Records (chi doc)
 *
 * ⚠ KHÔNG chứa logic tính lương, trừ phép, hay truy cập CSV trực tiếp.
 * ⚠ Mọi thao tác nghiệp vụ đều gọi qua Controller.
 */
public class MainView {

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final PayrollController    payrollController;
    private final LeaveController      leaveController;
    private final EmployeeController   employeeController;
    private final AuthController       authController;
    private final DepartmentController departmentController;
    private final AttendanceController attendanceController;
    private final Scanner              scanner;

    // Sub-views — khởi tạo lazy để tránh circular dependency
    private PayrollView    payrollView;
    private LeaveView      leaveView;
    private EmployeeView   employeeView;
    private ReportView     reportView;
    private DepartmentView departmentView;
    private AttendanceView attendanceView;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MainView(PayrollController payrollController,
                    LeaveController leaveController,
                    EmployeeController employeeController,
                    AuthController authController,
                    DepartmentController departmentController,
                    AttendanceController attendanceController) {
        this.payrollController    = payrollController;
        this.leaveController      = leaveController;
        this.employeeController   = employeeController;
        this.authController       = authController;
        this.departmentController = departmentController;
        this.attendanceController = attendanceController;
        this.scanner               = new Scanner(System.in);
    }

    // ─── Entry point ─────────────────────────────────────────────────────────

    /**
     * Khởi chạy ứng dụng — gọi từ Main.java.
     */
    public void start() {
        initSubViews();
        printWelcome();

        boolean appRunning = true;
        while (appRunning) {

            // ── Chưa đăng nhập → bắt buộc login ─────────────────────────────
            if (!authController.isLoggedIn()) {
                boolean loggedIn = showLoginScreen();
                if (!loggedIn) {
                    appRunning = false;
                    continue;
                }
            }

            // ── Đã đăng nhập → hiện menu theo role ───────────────────────────
            User user = authController.getCurrentUser();
            System.out.printf("%n  Xin chao %s! (Vai tro: %s)%n",
                    user.getUsername(), user.getRole());

            boolean sessionRunning = true;
            while (sessionRunning) {
                printMenuByRole(user.getRole());
                int choice = readInt("Nhap lua chon: ");

                // Logout
                if (choice == 9) {
                    authController.logout();
                    System.out.println("  Da dang xuat.");
                    sessionRunning = false;
                    continue;
                }

                // Thoát hẳn chương trình
                if (choice == 0) {
                    authController.logout();
                    appRunning = false;
                    sessionRunning = false;
                    continue;
                }

                // Xử lý menu theo role
                handleMenuByRole(user.getRole(), choice);
            }
        }

        System.out.println("\n  Tam biet! He thong da dong.\n");
        scanner.close();
    }

    // ─── Login screen ────────────────────────────────────────────────────────

    private boolean showLoginScreen() {
        while (true) {
            System.out.println("--------------------------------------------");
            System.out.println("|              DANG NHAP                   |");
            System.out.println("|  Nhap 'exit' o Username de thoat.        |");
            System.out.println("--------------------------------------------");

            System.out.print("  Username: ");
            String username = scanner.nextLine().trim();

            if (username.equalsIgnoreCase("exit")) {
                return false;
            }

            System.out.print("  Password: ");
            String password = scanner.nextLine().trim();

            if (authController.login(username, password)) {
                System.out.println("  Dang nhap thanh cong!");
                return true;
            } else {
                System.out.println("  [!] Sai username hoac password. Vui long thu lai.");
            }
        }
    }

    // ─── Menu theo Role ──────────────────────────────────────────────────────

    private void printMenuByRole(UserRole role) {
        switch (role) {
            case EMPLOYEE:      printEmployeeMenu();     break;
            case HR_STAFF:      printHrMenu();           break;
            case PAYROLL_STAFF: printPayrollStaffMenu(); break;
        }
    }

    private void printEmployeeMenu() {
        System.out.println("--------------------------------------------");
        System.out.println("|         MENU NHAN VIEN (EMPLOYEE)        |");
        System.out.println("|------------------------------------------|");
        System.out.println("|  1. Quan ly nghi phep                    |");
        System.out.println("|     (Nop don / Xem so du phep)           |");
        System.out.println("|  2. Cham cong                            |");
        System.out.println("|     (Check in/out / Xem cong /           |");
        System.out.println("|      Gui yeu cau dieu chinh cong)        |");
        System.out.println("|  3. Xem luong cua toi                    |");
        System.out.println("|  9. Dang xuat                            |");
        System.out.println("|  0. Thoat chuong trinh                   |");
        System.out.println("--------------------------------------------");
    }

    private void printHrMenu() {
        System.out.println("--------------------------------------------");
        System.out.println("|           MENU HR STAFF                  |");
        System.out.println("|------------------------------------------|");
        System.out.println("|  1. Quan ly nhan vien                    |");
        System.out.println("|  2. Quan ly phong ban                    |");
        System.out.println("|  3. Duyet / Tu choi don nghi phep        |");
        System.out.println("|  4. Quan ly cham cong                    |");
        System.out.println("|     (Xem tong hop / Duyet dieu chinh)    |");
        System.out.println("|  5. Bao cao tong ket                     |");
        System.out.println("|  9. Dang xuat                            |");
        System.out.println("|  0. Thoat chuong trinh                   |");
        System.out.println("--------------------------------------------");
    }

    private void printPayrollStaffMenu() {
        System.out.println("--------------------------------------------");
        System.out.println("|         MENU PAYROLL STAFF               |");
        System.out.println("|------------------------------------------|");
        System.out.println("|  1. Quan ly bang luong                   |");
        System.out.println("|     (Chay luong / Kiem toan / Lich su)   |");
        System.out.println("|  2. Bao cao & xuat du lieu                |");
        System.out.println("|  3. Xem danh sach cham cong (chi doc)     |");
        System.out.println("|  9. Dang xuat                            |");
        System.out.println("|  0. Thoat chuong trinh                   |");
        System.out.println("--------------------------------------------");
    }

    // ─── Xử lý lựa chọn theo role ────────────────────────────────────────────

    private void handleMenuByRole(UserRole role, int choice) {
        switch (role) {
            case EMPLOYEE:      handleEmployeeChoice(choice); break;
            case HR_STAFF:      handleHrChoice(choice);       break;
            case PAYROLL_STAFF: handlePayrollChoice(choice);  break;
        }
    }

    private void handleEmployeeChoice(int choice) {
    String currentEmpId = authController.getCurrentUser().getId(); // với role EMPLOYEE, userId == empId
    switch (choice) {
        case 1: leaveView.viewOwnBalance(currentEmpId);                  break;
        case 2: attendanceView.showEmployeeMenu();             break;
        case 3: payrollView.showEmployeeMenu(currentEmpId);    break;
        default: System.out.println("  [!] Lua chon khong hop le.");
    }
}

    private void handleHrChoice(int choice) {
        switch (choice) {
            case 1: employeeView.show();          break;
            case 2: departmentView.show();        break;
            case 3: leaveView.showHrMenu();       break;
            case 4: attendanceView.showHrMenu();  break;
            case 5: reportView.show();            break;
            default: System.out.println("  [!] Lua chon khong hop le.");
        }
    }

    private void handlePayrollChoice(int choice) {
        switch (choice) {
            case 1: payrollView.show();               break;
            case 2: reportView.show();                break;
            case 3: attendanceView.showReadOnlyMenu(); break;
            default: System.out.println("  [!] Lua chon khong hop le.");
        }
    }

    // ─── Print helpers ────────────────────────────────────────────────────────

    private void printWelcome() {
        System.out.println();
        System.out.println("----------------------------------------------------------");
        System.out.println("|     EMPLOYEE PAYROLL MANAGEMENT SYSTEM                  |");
        System.out.println("|     LAB211 — FPT University                             |");
        System.out.println("----------------------------------------------------------");
        System.out.println();
    }

    // ─── Input helpers (dùng chung cho toàn bộ View layer) ───────────────────

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Vui long nhap so nguyen.");
            }
        }
    }

    public String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("  [!] Khong duoc de trong.");
        }
    }

    public String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public Scanner getScanner() { return scanner; }

    // ─── Sub-view init ────────────────────────────────────────────────────────

    private void initSubViews() {
        this.payrollView    = new PayrollView(payrollController, this);
        this.leaveView      = new LeaveView(leaveController, this);
        this.employeeView   = new EmployeeView(employeeController, this);
        this.departmentView = new DepartmentView(departmentController, this);
        this.attendanceView = new AttendanceView(attendanceController, this);
        this.reportView     = new ReportView(
                payrollController, departmentController, attendanceController, this);
    }
}
