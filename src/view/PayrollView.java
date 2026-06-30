package view;

import controller.PayrollController;
import model.PayrollEntry;
import model.PayrollRun;

import java.util.List;

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
            int choice = main.readInt("Nhap lua chon: ");

            switch (choice) {
                case 1: runPayroll();         break;
                case 2: viewMonthlyTable();   break;
                case 3: viewEmployeeDetail(); break;
                case 4: viewAllRuns();        break;
                case 0: back = true;          break;
                default: System.out.println("  [!] Lua chon không hop le.");
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("\n-------------------------------------------");
        System.out.println(  "|          QUAN LY BANG LUONG              |");
        System.out.println(  "|------------------------------------------|");
        System.out.println(  "|  1. Chay bang luong thang                |");
        System.out.println(  "|  2. Xem bang luong theo thang            |");
        System.out.println(  "|  3. Xem chi tiet lương nhan vien         |");
        System.out.println(  "|  4. Lich su cac dot chay luong           |");
        System.out.println(  "|  0. Quay lai Menu chinh                  |");
        System.out.println(  "--------------------------------------------");
        
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void runPayroll() {
        System.out.println("\n  === CHAY BANG LUONG ===");
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam (vd: 2024): ");

        System.out.printf("  Dang chay bang luong %d/%d...%n", month, year);

        try {
            PayrollController.RunResult result = controller.runPayroll(month, year);
            System.out.println("\n  ✓ Hoan thanh!");
            printRunSummary(result);
        } catch (IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void viewMonthlyTable() {
        System.out.println("\n  === BANG LUONG THEO THANG ===");
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam (vd: 2024): ");

        List<PayrollEntry> entries = controller.getEntriesByMonthYear(month, year);

        if (entries.isEmpty()) {
            System.out.printf("  [!] Chua co du lieu luong thang %d/%d.%n", month, year);
            return;
        }

        printPayrollTable(entries, month, year);
    }

    private void viewEmployeeDetail() {
        System.out.println("\n  === CHI TIET LUONG NHAN VIEN ===");
        String empId = main.readString("  Ma nhan vien (vd: EMP0001): ");
        int month    = main.readInt("  Thang (1-12): ");
        int year     = main.readInt("  Nam (vd: 2024): ");

        PayrollEntry entry = controller.getEntryByEmpAndMonth(empId, month, year);

        if (entry == null) {
            System.out.printf("  [!] Khong tim thay du lieu luong cua %s thang %d/%d.%n",
                empId, month, year);
            return;
        }

        printEntryDetail(entry);
    }

    private void viewAllRuns() {
        System.out.println("\n  === LICH SU DOT CHAY LUONG ===");
        List<PayrollRun> runs = controller.getAllRuns();

        if (runs.isEmpty()) {
            System.out.println("  Chua co dot chay luong nao.");
            return;
        }

        System.out.printf("  %-12s %-8s %-12s %-10s %-20s%n",
            "Run ID", "Thang", "Nhan vien", "Status", "Tong luong (VND)");
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

    private void printRunSummary(PayrollController.RunResult result) {
        System.out.printf("  Run ID     : %s%n",      result.runId);
        System.out.printf("  Nhan vien  : %d%n",      result.processedCount);
        System.out.printf("  Bo qua     : %d%n",      result.skippedCount);
        System.out.printf("  Tong luong : %,d VNĐ%n", result.totalNetPay);
        System.out.println();
    }

    private void printPayrollTable(List<PayrollEntry> entries, int month, int year) {
        System.out.printf("%n  BANG LUONG THANG %d/%d — %d nhan vien%n",
            month, year, entries.size());
        System.out.println("  " + "═".repeat(90));
        System.out.printf("  %-10s %-8s %-14s %-14s %-14s %-14s %-10s%n",
            "Emp ID", "Phong", "Luong co ban", "OT + Bonus", "Khau tru", "Thue", "Thuc nhan");
        System.out.println("  " + "─".repeat(90));

        long totalNet = 0;
        for (PayrollEntry e : entries) {
            long otAndBonus = e.getOvertimePay() + e.getBonus();
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
        System.out.printf("  %-43s TONG THUC NHAN: %,d VND%n", "", totalNet);
        System.out.println();
    }

    private void printEntryDetail(PayrollEntry e) {
        System.out.println("\n  ┌─────────────────────────────────────────┐");
        System.out.printf( "  │  CHI TIET LUONG: %-24s│%n", e.getEmpId());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  Phong ban     : %-24s│%n", e.getDeptId());
        System.out.printf( "  │  Thang/Nam     : %02d/%-21d│%n", e.getMonth(), e.getYear());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  Luong co ban  : %,24d │%n", e.getBaseSalary());
        System.out.printf( "  │  Lương OT      : %,24d │%n", e.getOvertimePay());
        System.out.printf( "  │  Thuong chuyen can: %,21d │%n", e.getBonus());
        System.out.printf( "  │  Khau tru vang : %,24d │%n", e.getAbsenceDeduction());
        System.out.printf( "  │  Thue TNCN     : %,24d │%n", e.getTaxAmount());
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.printf( "  │  LUONG THUC NHAN: %,23d │%n", e.getNetSalary());
        System.out.printf( "  │  Trang thai    : %-24s│%n", e.getStatus());
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();
    }
}