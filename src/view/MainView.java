package view;

import controller.PayrollController;
import controller.LeaveController;
import controller.EmployeeController;

import java.util.Scanner;

/**
 * MainView – Menu console chính của hệ thống Payroll.
 *
 * Tuần 6: Kết nối View ↔ Controller ↔ Repository đúng MVC.
 *
 * ⚠ KHÔNG chứa logic tính lương, trừ phép, hay truy cập CSV trực tiếp.
 * ⚠ Mọi thao tác nghiệp vụ đều gọi qua Controller.
 */
public class MainView {

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final PayrollController    payrollController;
    private final LeaveController      leaveController;
    private final EmployeeController   employeeController;
    private final Scanner              scanner;

    // Sub-views — khởi tạo lazy để tránh circular dependency
    private PayrollView    payrollView;
    private LeaveView      leaveView;
    private EmployeeView   employeeView;
    private ReportView     reportView;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MainView(PayrollController payrollController,
                    LeaveController leaveController,
                    EmployeeController employeeController) {
        this.payrollController  = payrollController;
        this.leaveController    = leaveController;
        this.employeeController = employeeController;
        this.scanner            = new Scanner(System.in);
    }

    // ─── Entry point ─────────────────────────────────────────────────────────

    /**
     * Khởi chạy ứng dụng — gọi từ Main.java.
     */
    public void start() {
        initSubViews();
        printWelcome();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Nhập lựa chọn: ");

            switch (choice) {
                case 1:
                    payrollView.show();
                    break;
                case 2:
                    leaveView.show();
                    break;
                case 3:
                    employeeView.show();
                    break;
                case 4:
                    reportView.show();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    System.out.println("  [!] Lựa chọn không hợp lệ. Thử lại.");
            }
        }

        System.out.println("\n  Tạm biệt! Hệ thống đã đóng.\n");
        scanner.close();
    }

    // ─── Print helpers ────────────────────────────────────────────────────────

    private void printWelcome() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║     EMPLOYEE PAYROLL MANAGEMENT SYSTEM               ║");
        System.out.println("║     LAB211 — FPT University                          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printMainMenu() {
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.println("│              MENU CHÍNH                  │");
        System.out.println("├──────────────────────────────────────────┤");
        System.out.println("│  1. Quản lý Bảng lương (Payroll)         │");
        System.out.println("│  2. Quản lý Nghỉ phép  (Leave)           │");
        System.out.println("│  3. Quản lý Nhân viên  (Employee)        │");
        System.out.println("│  4. Báo cáo tổng kết   (Report)          │");
        System.out.println("│  0. Thoát                                │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    // ─── Input helpers (dùng chung cho toàn bộ View layer) ───────────────────

    /**
     * Đọc số nguyên từ console, bỏ qua input không hợp lệ.
     */
    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Vui lòng nhập số nguyên.");
            }
        }
    }

    /**
     * Đọc chuỗi không rỗng từ console.
     */
    public String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("  [!] Không được để trống.");
        }
    }

    /**
     * Đọc chuỗi cho phép rỗng (dùng cho các trường optional).
     */
    public String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public Scanner getScanner() { return scanner; }

    // ─── Sub-view init ────────────────────────────────────────────────────────

    private void initSubViews() {
        this.payrollView  = new PayrollView(payrollController, this);
        this.leaveView    = new LeaveView(leaveController, this);
        this.employeeView = new EmployeeView(employeeController, this);
        this.reportView   = new ReportView(payrollController, this);
    }
}
