package view;

import controller.AttendanceController;
import controller.AttendanceController.AdjustmentRequest;
import model.AttendanceRecord;

import java.util.List;

public class AttendanceView {

    private final AttendanceController controller;
    private final MainView main;

    public AttendanceView(AttendanceController controller, MainView main) {
        this.controller = controller;
        this.main = main;
    }

    // ─── Menu cho Employee ────────────────────────────────────────────────────
    public void showEmployeeMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|         CHAM CONG (EMPLOYEE)             |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "|  1. Check in                             |");
            System.out.println(  "|  2. Check out                            |");
            System.out.println(  "|  3. Xem bang cham cong cua toi           |");
            System.out.println(  "|  4. Gui yeu cau dieu chinh cong           |");
            System.out.println(  "|  0. Quay lai                             |");
            System.out.println(  "--------------------------------------------");
            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: checkIn();          break;
                case 2: checkOut();         break;
                case 3: viewMyAttendance(); break;
                case 4: submitAdjustment(); break;
                case 0: back = true;        break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu cho HR Staff (đầy đủ, có duyệt/từ chối) ────────────────────────
    public void showHrMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|      QUAN LY CHAM CONG (HR STAFF)        |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "|  1. Xem tat ca ban ghi cham cong          |");
            System.out.println(  "|  2. Xem tong hop cham cong theo thang     |");
            System.out.println(  "|  3. Xem yeu cau dieu chinh cho duyet      |");
            System.out.println(  "|  4. Duyet yeu cau dieu chinh              |");
            System.out.println(  "|  5. Tu choi yeu cau dieu chinh            |");
            System.out.println(  "|  0. Quay lai                             |");
            System.out.println(  "--------------------------------------------");
            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: listAll();                break;
                case 2: viewSummary();            break;
                case 3: listPendingAdjustments(); break;
                case 4: approveAdjustment();      break;
                case 5: rejectAdjustment();       break;
                case 0: back = true;              break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu chỉ đọc cho Payroll Staff (List Attendance Records) ────────────
    public void showReadOnlyMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|   DANH SACH CHAM CONG (PAYROLL STAFF)    |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "|  1. Xem tat ca ban ghi cham cong          |");
            System.out.println(  "|  2. Xem tong hop cham cong theo thang     |");
            System.out.println(  "|  0. Quay lai                             |");
            System.out.println(  "--------------------------------------------");
            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: listAll();     break;
                case 2: viewSummary(); break;
                case 0: back = true;   break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void checkIn() {
        String empId = main.readString("  Ma nhan vien: ");
        try {
            AttendanceRecord r = controller.checkIn(empId);
            System.out.println("  ✓ Check in thanh cong. Ngay cong thang nay: " + r.getWorkDays());
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void checkOut() {
        String empId = main.readString("  Ma nhan vien: ");
        controller.checkOut(empId);
        System.out.println("  ✓ Check out thanh cong.");
    }

    private void viewMyAttendance() {
        String empId = main.readString("  Ma nhan vien: ");
        List<AttendanceRecord> list = controller.findByEmployeeId(empId);
        if (list.isEmpty()) {
            System.out.println("  [!] Chua co du lieu cham cong.");
            return;
        }
        list.forEach(r -> System.out.printf(
            "  %d/%d | Ngay cong: %d | Vang: %d | OT: %dh | Tre: %d%n",
            r.getMonth(), r.getYear(), r.getWorkDays(), r.getAbsenceDays(),
            r.getOvertimeHours(), r.getLateCount()));
    }

    private void submitAdjustment() {
        try {
            String empId       = main.readString("  Ma nhan vien: ");
            int month           = main.readInt("  Thang (1-12): ");
            int year             = main.readInt("  Nam: ");
            int workDays         = main.readInt("  So ngay cong de nghi: ");
            int absenceDays      = main.readInt("  So ngay vang de nghi: ");
            int otHours          = main.readInt("  So gio OT de nghi: ");
            String reason        = main.readString("  Ly do dieu chinh: ");

            String reqId = controller.submitAdjustmentRequest(
                empId, month, year, workDays, absenceDays, otHours, reason);
            System.out.println("  ✓ Da gui yeu cau dieu chinh: " + reqId + " (cho HR duyet)");
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void listAll() {
        List<AttendanceRecord> list = controller.getAll();
        if (list.isEmpty()) {
            System.out.println("  Chua co du lieu cham cong.");
            return;
        }
        list.forEach(r -> System.out.printf(
            "  %s | %s | %d/%d | Ngay cong: %d | Vang: %d | OT: %dh%n",
            r.getId(), r.getEmpId(), r.getMonth(), r.getYear(),
            r.getWorkDays(), r.getAbsenceDays(), r.getOvertimeHours()));
    }

    private void viewSummary() {
        int month = main.readInt("  Thang (1-12): ");
        int year  = main.readInt("  Nam: ");
        System.out.println("  " + controller.summarize(month, year));
    }

    private void listPendingAdjustments() {
        List<AdjustmentRequest> list = controller.getPendingRequests();
        if (list.isEmpty()) {
            System.out.println("  Khong co yeu cau nao dang cho duyet.");
            return;
        }
        list.forEach(r -> System.out.println("  " + r));
    }

    private void approveAdjustment() {
        String reqId      = main.readString("  Ma yeu cau: ");
        String approverId = main.readString("  Ma nguoi duyet: ");
        boolean ok = controller.approveAdjustment(reqId, approverId);
        System.out.println(ok ? "  ✓ Da duyet yeu cau." : "  [!] Khong tim thay yeu cau.");
    }

    private void rejectAdjustment() {
        String reqId      = main.readString("  Ma yeu cau: ");
        String approverId = main.readString("  Ma nguoi tu choi: ");
        boolean ok = controller.rejectAdjustment(reqId, approverId);
        System.out.println(ok ? "  ✓ Da tu choi yeu cau." : "  [!] Khong tim thay yeu cau.");
    }
}
