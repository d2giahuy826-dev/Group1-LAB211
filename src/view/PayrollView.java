package view;

import controller.PayrollController;
import model.PayrollEntry;
import model.PayrollRun;

import java.util.List;
import java.util.Optional;

/**
 * PayrollView – Giao diện console quản lý Bảng lương.
 *
 * ⚠ KHÔNG chứa công thức tính lương — chỉ hiển thị kết quả từ Controller.
 * ⚠ KHÔNG truy cập CSV trực tiếp.
 */
public class PayrollView {

    private final PayrollController controller;
    private final MainView          main;

    public PayrollView(PayrollController controller, MainView main) {
        this.controller = controller;
        this.main       = main;
    }

    // ─── Entry point ─────────────────────────────────────────────────────────

    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = main.readInt("Nhập lựa chọn: ");

            switch (choice) {
                case 1: runPayroll();         break;
                case 2: viewMonthlyTable();   break;
                case 3: viewEmployeeDetail(); break;
                case 4: viewAllRuns();        break;
                case 0: back = true;          break;
                default: System.out.println("  [!] Lựa chọn không hợp lệ.");
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("\n┌──────────────────────────────────────────┐");
        System.out.println(  "│          QUẢN LÝ BẢNG LƯƠNG             │");
        System.out.println(  "├──────────────────────────────────────────┤");
        System.out.println(  "│  1. Chạy bảng lương tháng               │");
        System.out.println(  "│  2. Xem bảng lương theo tháng            │");
        System.out.println(  "│  3. Xem chi tiết lương nhân viên         │");
        System.out.println(  "│  4. Lịch sử các đợt chạy lương           │");
        System.out.println(  "│  0. Quay lại Menu chính                  │");
        System.out.println(  "└──────────────────────────────────────────┘");
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void runPayroll() {
        System.out.println("\n  === CHẠY BẢNG LƯƠNG ===");
        int month = main.readInt("  Tháng (1-12): ");
        int year  = main.readInt("  Năm (vd: 2024): ");

        System.out.printf("  Đang chạy bảng lương %d/%d...%n", month, year);

        try {
            PayrollRun run = controller.runPayroll(month, year);
            System.out.println("\n  ✓ Hoàn thành!");
            printRunSummary(run);
        } catch (IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  [!] Lỗi: " + e.getMessage());
        }
    }

    private void viewMonthlyTable() {
        System.out.println("\n  === BẢNG LƯƠNG THEO THÁNG ===");
        int month = main.readInt("  Tháng (1-12): ");
        int year  = main.readInt("  Năm (vd: 2024): ");

        List<PayrollEntry> entries = controller.getEntriesByMonthYear(month, year);

        if (entries.isEmpty()) {
            System.out.printf("  [!] Chưa có dữ liệu lương tháng %d/%d.%n", month, year);
            return;
        }

        printPayrollTable(entries, month, year);
    }

    private void viewEmployeeDetail() {
        System.out.println("\n  === CHI TIẾT LƯƠNG NHÂN VIÊN ===");
        String empId = main.readString("  Mã nhân viên (vd: EMP0001): ");
        int month    = main.readInt("  Tháng (1-12): ");
        int year     = main.readInt("  Năm (vd: 2024): ");

        PayrollEntry entry = controller.getEntryByEmpAndMonth(empId, month, year);

        if (entry == null) {
            System.out.printf("  [!] Không tìm thấy dữ liệu lương của %s tháng %d/%d.%n",
                empId, month, year);
            return;
        }

        printEntryDetail(entry);
    }

    private void viewAllRuns() {
        System.out.println("\n  === LỊCH SỬ ĐỢT CHẠY LƯƠNG ===");
        List<PayrollRun> runs = controller.getAllRuns();

        if (runs.isEmpty()) {
            System.out.println("  Chưa có đợt chạy lương nào.");
            return;
        }

        System.out.printf("  %-12s %-8s %-12s %-10s %-20s%n",
            "Run ID", "Tháng", "Nhân viên", "Status", "Tổng lương (VNĐ)");
        System.out.println("  " + "─".repeat(68));

        for (PayrollRun run : runs) {
            System.out.printf("  %-12s %2d/%-5d %-12d %-10s %,20d%n",
                run.getId(),
                run.getMonth(), run.getYear(),
                run.getTotalEmployees(),
                run.getStatus(),
                run.getTotalNetPay());
        }
        System.out.println();
    }

    // ─── Print helpers ────────────────────────────────────────────────────────

    private void printPayrollTable(List<PayrollEntry> entries, int month, int year) {
        System.out.printf("%n  BẢNG LƯƠNG THÁNG %d/%d — %d nhân viên%n", month, year, entries.size());
        System.out.println("  " + "═".repeat(90));
        System.out.printf("  %-10s %-8s %-14s %-14s %-14s %-14s %-10s%n",
            "Emp ID", "Phòng", "Lương cơ bản", "OT + Bonus", "Khấu trừ", "Thuế", "Thực nhận");
        System.out.println("  " + "─".repeat(90));

        long totalNet = 0;
        for (PayrollEntry e : entries) {
            long otAndBonus = e.getOvertimePay() + e.getBonus();
            long deductions = e.getAbsenceDeduction() + e.getTaxAmount();
            System.out.printf("  %-10s %-8s %,14d %,14d %,14d %,14d %,10d%n",
                e.getEmpId(),
                e.getDeptId(),
                e.getBaseSalary(),
                otAndBonus,
                e.getAbsenceDeduction(),
                e.getTaxAmount(),
                e.getNetSalary());
            totalNet += e.getNetSalary();
        }

        System.out.println("  " + "─".repeat(90));
        System.out.printf("  %-43s TỔNG THỰC NHẬN: %,d VNĐ%n", "", totalNet);
        System.out.println();
    }

    private void printEntryDetail(PayrollEntry e) {
        System.out.println("\n  ┌─────────────────────────────────────────┐");
        System.out.printf( "  │  CHI TIẾT LƯƠNG: %-24s│%n", e.getEmpId());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  Phòng ban     : %-24s│%n", e.getDeptId());
        System.out.printf( "  │  Tháng/Năm     : %02d/%-21d│%n", e.getMonth(), e.getYear());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  Lương cơ bản  : %,24d │%n", e.getBaseSalary());
        System.out.printf( "  │  Lương OT      : %,24d │%n", e.getOvertimePay());
        System.out.printf( "  │  Thưởng chuyên cần: %,21d │%n", e.getBonus());
        System.out.printf( "  │  Khấu trừ vắng : %,24d │%n", e.getAbsenceDeduction());
        System.out.printf( "  │  Thuế TNCN     : %,24d │%n", e.getTaxAmount());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  LƯƠNG THỰC NHẬN: %,23d │%n", e.getNetSalary());
        System.out.printf( "  │  Trạng thái    : %-24s│%n", e.getStatus());
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();
    }

    private void printRunSummary(PayrollRun run) {
        System.out.printf("  Run ID     : %s%n",    run.getId());
        System.out.printf("  Tháng/Năm  : %d/%d%n", run.getMonth(), run.getYear());
        System.out.printf("  Nhân viên  : %d%n",    run.getTotalEmployees());
        System.out.printf("  Tổng lương : %,d VNĐ%n", run.getTotalNetPay());
        System.out.printf("  Hoàn thành : %s%n",    run.getCompletedAt());
        System.out.println();
    }
}
