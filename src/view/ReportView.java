package view;

import controller.PayrollController;
import repository.EmployeeRepository;
import repository.LeaveRequestRepository;
import model.Employee;
import model.LeaveRequest;
import model.PayrollEntry;
import model.PayrollRun;

import java.util.List;

public class ReportView {

    private final PayrollController       payrollController;
    private final MainView                main;

    // Các repository phụ trợ để lấy dữ liệu tổng hợp
    private final EmployeeRepository      employeeRepo     = new EmployeeRepository();
    private final LeaveRequestRepository  leaveRequestRepo = new LeaveRequestRepository();
   
         
    // ─── Constructor (Dependency Injection từ MainView) ───────────────────────
    public ReportView(PayrollController payrollController, MainView main) {
        this.payrollController = payrollController;
        this.main              = main;
    }

    // ─── Entry point ──────────────────────────────────────────────────────────
    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = main.readInt("Nhap lua chon: ");

            switch (choice) {
                case 1: reportMonthlySummary(); break;
                case 2: reportPayrollHistory(); break;
                case 3: reportEmployeeSummary(); break;
                case 0: back = true;            break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────
    private void printMenu() {
        System.out.println("\n------------------------------------------");
        System.out.println(  "|         BAO CAO TONG KET                |");
        System.out.println(  "|-----------------------------------------|");
        System.out.println(  "| 1. Bao cao tong ket thang               |");
        System.out.println(  "|  2. Lich su cac dot chay luong          |");
        System.out.println(  "|  3. Tong hop nhan vien                  |");
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

        long totalNet        = 0;
        long totalBonus      = 0;
        long totalDeduction  = 0;
        long totalTax        = 0;

        for (PayrollEntry e : entries) {
            totalNet       += e.getNetSalary();
            totalBonus     += e.getBonus();
            totalDeduction += e.getAbsenceDeduction();
            totalTax       += e.getTaxAmount();
        }

        System.out.println("\n  " + "═".repeat(48));
        System.out.printf( "  BAO CAO LUONG THANG %d/%d%n", month, year);
        System.out.println("  " + "─".repeat(48));
        System.out.printf( "  So nhan vien xu ly  : %d%n",      entries.size());
        System.out.printf( "  Tong thuong chuyen can: %,d VND%n", totalBonus);
        System.out.printf( "  Tong khau tru vang   : %,d VND%n", totalDeduction);
        System.out.printf( "  Tong thue TNCN       : %,d VND%n", totalTax);
        System.out.println("  " + "─".repeat(48));
        System.out.printf( "  TONG THUC NHAN       : %,d VND%n", totalNet);
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

        List<Employee>     employees     = employeeRepo.loadAll();
        List<LeaveRequest> leaveRequests = leaveRequestRepo.loadAll();

        long activeCount   = employees.stream().filter(Employee::isActive).count();
        long inactiveCount = employees.size() - activeCount;

        long pendingLeaves = leaveRequests.stream()
                .filter(r -> r.getStatus() == model.LeaveStatus.PENDING)
                .count();
        long approvedLeaves = leaveRequests.stream()
                .filter(r -> r.getStatus() == model.LeaveStatus.APPROVED)
                .count();

        System.out.println("\n  " + "═".repeat(40));
        System.out.println("  TONG HOP NHAN VIEN & NGHI PHEP");
        System.out.println("  " + "─".repeat(40));
        System.out.printf( "  Tong nhan vien  : %d%n",   employees.size());
        System.out.printf( "  Dang lam viec   : %d%n",   activeCount);
        System.out.printf( "  Da nghi viec    : %d%n",   inactiveCount);
        System.out.println("  " + "─".repeat(40));
        System.out.printf( "  Tong don phep   : %d%n",   leaveRequests.size());
        System.out.printf( "  Cho duyet       : %d%n",   pendingLeaves);
        System.out.printf( "  Da duyet        : %d%n",   approvedLeaves);
        System.out.println("  " + "═".repeat(40));
        System.out.println();
    }
}