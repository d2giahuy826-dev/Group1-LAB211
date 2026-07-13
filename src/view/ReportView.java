package view;

import controller.AttendanceController;
import controller.DepartmentController;
import controller.PayrollController;
import model.Department;
import model.Employee;
import model.LeaveRequest;
import model.LeaveStatus;
import model.PayrollEntry;
import model.PayrollRun;

import java.util.List;

/**
 * ReportView – Hiển thị báo cáo tổng kết (Reporting & Data Management Area).
 *
 * ✅ Đúng chuẩn MVC: KHÔNG gọi Repository trực tiếp.
 * Toàn bộ data lấy qua Controller.
 */
public class ReportView {

    private final PayrollController    payrollController;
    private final DepartmentController departmentController;
    private final AttendanceController attendanceController;
    private final MainView             main;

    // ─── Constructor (Dependency Injection từ MainView) ───────────────────────
    public ReportView(PayrollController payrollController,
                      DepartmentController departmentController,
                      AttendanceController attendanceController,
                      MainView main) {
        this.payrollController    = payrollController;
        this.departmentController = departmentController;
        this.attendanceController = attendanceController;
        this.main                 = main;
    }

    // ─── Entry point ──────────────────────────────────────────────────────────
    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = main.readInt("Nhap lua chon: ");

            switch (choice) {
                case 1: reportMonthlySummary();  break;
                case 2: reportPayrollHistory();  break;
                case 3: reportEmployeeSummary(); break;
                case 4: reportDepartment();      break;
                case 5: reportAttendance();      break;
                case 6: exportPayrollCsv();      break;
                case 0: back = true;             break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────
    private void printMenu() {
        System.out.println("\n------------------------------------------");
        System.out.println(  "|         BAO CAO TONG KET                |");
        System.out.println(  "|-----------------------------------------|");
        System.out.println(  "|  1. Bao cao tong ket luong thang         |");
        System.out.println(  "|  2. Lich su cac dot chay luong           |");
        System.out.println(  "|  3. Tong hop nhan vien                   |");
        System.out.println(  "|  4. Bao cao phong ban                   |");
        System.out.println(  "|  5. Bao cao cham cong                   |");
        System.out.println(  "|  6. Xuat bao cao luong ra CSV            |");
        System.out.println(  "|  0. Quay lai Menu chinh                 |");
        System.out.println(  "-------------------------------------------");
    }

    // ─── Report 1: Tổng kết tháng ─────────────────────────────────────────────
    private void reportMonthlySummary() {
        System.out.println("\n  === BAO CAO TONG KET THANG ===");
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam (vd: 2024): ");

        List<PayrollEntry> entries = payrollController.getEntriesByMonthYear(month, year);

        if (entries.isEmpty()) {
            System.out.printf("  [!] Chua co du lieu luong thang %d/%d.%n", month, year);
            return;
        }

        long totalNet       = 0;
        long totalBonus     = 0;
        long totalDeduction = 0;
        long totalTax       = 0;

        for (PayrollEntry e : entries) {
            totalNet       += e.getNetSalary();
            totalBonus     += e.getBonus();
            totalDeduction += e.getAbsenceDeduction();
            totalTax       += e.getTaxAmount();
        }

        System.out.println("\n  " + "═".repeat(48));
        System.out.printf( "  BAO CAO LUONG THANG %d/%d%n", month, year);
        System.out.println("  " + "─".repeat(48));
        System.out.printf( "  So nhan vien xu ly    : %d%n",      entries.size());
        System.out.printf( "  Tong thuong chuyen can: %,d VND%n", totalBonus);
        System.out.printf( "  Tong khau tru vang    : %,d VND%n", totalDeduction);
        System.out.printf( "  Tong thue TNCN        : %,d VND%n", totalTax);
        System.out.println("  " + "─".repeat(48));
        System.out.printf( "  TONG THUC NHAN        : %,d VND%n", totalNet);
        System.out.println("  " + "═".repeat(48));
        System.out.println();
    }

    // ─── Report 2: Lịch sử chạy lương ────────────────────────────────────────
    private void reportPayrollHistory() {
        System.out.println("\n  === LICH SU CAC DOT CHAY LUONG ===");

        List<PayrollRun> runs = payrollController.getAllRuns();

        if (runs.isEmpty()) {
            System.out.println("  Chua co dot chay luong nao.");
            return;
        }

        System.out.printf("  %-14s %-8s %-12s %-12s %-20s%n",
                "Run ID", "Thang", "Nhan vien", "Status", "Tong luong (VND)");
        System.out.println("  " + "─".repeat(72));

        for (PayrollRun run : runs) {
            System.out.printf("  %-14s %2d/%-5d %-12d %-12s %,20d%n",
                    run.getId(),
                    run.getMonth(), run.getYear(),
                    run.getTotalEmployees(),
                    run.getStatus(),
                    run.getTotalNetPay());
        }
        System.out.println();
    }

    // ─── Report 3: Tổng hợp nhân viên ────────────────────────────────────────
    private void reportEmployeeSummary() {
        System.out.println("\n  === TONG HOP NHAN VIEN ===");

        // ✅ Lấy data qua Controller, không gọi repo trực tiếp
        List<Employee>     employees     = payrollController.getAllEmployees();
        List<LeaveRequest> leaveRequests = payrollController.getAllLeaveRequests();

        long activeCount   = employees.stream().filter(Employee::isActive).count();
        long inactiveCount = employees.size() - activeCount;

        long pendingLeaves = leaveRequests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.PENDING)
                .count();
        long approvedLeaves = leaveRequests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED)
                .count();
        long rejectedLeaves = leaveRequests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.REJECTED)
                .count();        

        System.out.println("\n  " + "-".repeat(40));
        System.out.println("  TONG HOP NHAN VIEN & NGHI PHEP");
        System.out.println("  " + "-".repeat(40));
        System.out.printf( "  Tong nhan vien  : %d%n", employees.size());
        System.out.printf( "  Dang lam viec   : %d%n", activeCount);
        System.out.printf( "  Da nghi viec    : %d%n", inactiveCount);
        System.out.println("  " + "-".repeat(40));
        System.out.printf( "  Tong don phep   : %d%n", leaveRequests.size());
        System.out.printf( "  Cho duyet       : %d%n", pendingLeaves);
        System.out.printf( "  Da duyet        : %d%n", approvedLeaves);
        System.out.printf("  Da tu choi      : %d%n", rejectedLeaves);
        System.out.println("  " + "-".repeat(40));
    }

    // ─── Report 4: Báo cáo phòng ban ──────────────────────────────────────────
    private void reportDepartment() {
        System.out.println("\n  === BAO CAO PHONG BAN ===");
        List<Department> depts = departmentController.getAll();
        if (depts.isEmpty()) {
            System.out.println("  Chua co du lieu phong ban.");
            return;
        }
        System.out.printf("  %-8s %-20s %-10s %-12s %-15s%n",
            "Ma PB", "Ten phong ban", "Quan ly", "So NV", "Vi tri");
        System.out.println("  " + "-".repeat(70));
        for (Department d : depts) {
            System.out.printf("  %-8s %-20s %-10s %-12d %-15s%n",
                d.getId(), d.getName(), d.getManagerId(),
                d.getTotalEmployees(), d.getLocation());
        }
        System.out.println();
    }

    // ─── Report 5: Báo cáo chấm công ──────────────────────────────────────────
    private void reportAttendance() {
        System.out.println("\n  === BAO CAO CHAM CONG ===");
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam: ");
        System.out.println("  " + attendanceController.summarize(month, year));
        System.out.println();
    }

    // ─── Report 6: Export Payroll CSV ────────────────────────────────────────
    private void exportPayrollCsv() {
        System.out.println("\n  === XUAT BAO CAO LUONG RA CSV ===");
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam: ");
        String outputPath = main.readString("  Duong dan file xuat (vd: data/export_payroll.csv): ");

        try {
            String path = payrollController.exportPayrollCsv(month, year, outputPath);
            System.out.println("  ✓ Da xuat bao cao luong ra: " + path);
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }
}
