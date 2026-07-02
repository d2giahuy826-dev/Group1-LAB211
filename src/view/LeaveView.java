package view;

import controller.LeaveController;
import model.LeaveBalance;
import model.LeaveRequest;
import model.LeaveType;
import exception.InsufficientLeaveException;
import java.time.LocalDate;
import java.util.List;

public class LeaveView {

    private final LeaveController controller;
    private final MainView        main;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public LeaveView(LeaveController controller, MainView main) {
        this.controller = controller;
        this.main       = main;
    }

    // ─── Menu cho Employee ────────────────────────────────────────────────────
    public void showEmployeeMenu(String currentEmpId) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|      NGHI PHEP (EMPLOYEE)                |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "| 1. Nop don xin nghi phep                 |");
            System.out.println(  "| 2. Xem so du phep                        |");
            System.out.println(  "| 0. Quay lai Menu chinh                   |");
            System.out.println(  "--------------------------------------------");

            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: submitLeave();  break;
                case 2: viewOwnBalance(currentEmpId);  break;
                case 0: back = true;    break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Menu cho HR Staff ────────────────────────────────────────────────────
    public void showHrMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|      DUYET NGHI PHEP (HR STAFF)          |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "| 1. Xem danh sach don cho duyet            |");
            System.out.println(  "| 2. Duyet don nghi phep                   |");
            System.out.println(  "| 3. Tu choi don nghi phep                 |");
            System.out.println(  "| 0. Quay lai Menu chinh                   |");
            System.out.println(  "--------------------------------------------");

            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: listPending();      break;
                case 2: approveLeave();     break;
                case 3: rejectLeave();      break;
                case 0: back = true;        break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void submitLeave() {
        System.out.println("\n  === NOP DON XIN NGHI PHEP ===");
        try {
            String empId = main.readString("  Ma nhan vien: ");

            System.out.print("  Loai phep (ANNUAL/SICK): ");
            LeaveType type = LeaveType.fromString(main.getScanner().nextLine().trim());

            String startStr = main.readString("  Ngay bat dau (yyyy-MM-dd): ");
            LocalDate startDate = LocalDate.parse(startStr);

            String endStr = main.readString("  Ngay ket thuc (yyyy-MM-dd): ");
            LocalDate endDate = LocalDate.parse(endStr);

            int days = main.readInt("  So ngay nghi: ");

            String reason = main.readOptionalString("  Ly do (Enter de bo qua): ");

            controller.submitLeave(empId, type, startDate, endDate, days, reason);
            System.out.println("  ✓ Da nop don nghi phep thanh cong!");

        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void listPending() {
        System.out.println("\n  === DON NGHI PHEP CHO DUYET ===");
        List<LeaveRequest> list = controller.getPendingRequests();
        if (list.isEmpty()) {
            System.out.println("  Khong co don nao dang cho duyet.");
            return;
        }
        list.forEach(r -> System.out.printf(
            "  %s | %s | %s | %s -> %s | %d ngay | Ly do: %s%n",
            r.getId(), r.getEmpId(), r.getType(),
            r.getStartDate(), r.getEndDate(), r.getDays(), r.getReason()));
    }

    private void approveLeave() {
        System.out.println("\n  === DUYET DON NGHI PHEP ===");
        try {
            String requestId = main.readString("  Ma don nghi phep: ");
            String approverId = main.readString("  Ma nguoi duyet: ");

            controller.approve(requestId, approverId);
            System.out.println("  ✓ Da duyet don nghi phep thanh cong!");

        } catch (InsufficientLeaveException e) {
            System.out.println("  [!] Khong du ngay phep: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }

    private void rejectLeave() {
        System.out.println("\n  === TU CHOI DON NGHI PHEP ===");
        try {
            String requestId  = main.readString("  Ma don nghi phep: ");
            String approverId = main.readString("  Ma nguoi tu choi: ");

            controller.reject(requestId, approverId);
            System.out.println("  ✓ Da tu choi don nghi phep!");

        } catch (Exception e) {
            System.out.println("  [!] Loi: " + e.getMessage());
        }
    }
    /**
 * Dung rieng cho Employee — empId LAY TU SESSION dang nhap,
 * KHONG cho nhap tay de tranh xem so du phep cua nguoi khac.
 * Public de co the goi truc tiep tu MainView hoac view/class khac neu can.
 */
public void viewOwnBalance(String empId) {
    System.out.println("\n  === SO DU NGHI PHEP CUA TOI ===");
    LeaveBalance balance = controller.getBalance(empId);
    if (balance == null) {
        System.out.println("  [!] Khong tim thay so du phep cua ban.");
        return;
    }
    System.out.println("  ┌─────────────────────────────────────────┐");
    System.out.printf( "  │  Phep nam : con %d / tong %d ngay         │%n",
            balance.getAnnualRemaining(), balance.getAnnualTotal());
    System.out.printf( "  │  Nghi om  : con %d / tong %d ngay         │%n",
            balance.getSickRemaining(), balance.getSickTotal());
    System.out.println("  └─────────────────────────────────────────┘");
}
    
}
