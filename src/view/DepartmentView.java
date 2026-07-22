package view;

import controller.DepartmentController;
import model.Department;

import java.util.List;

public class DepartmentView {

    private final DepartmentController controller;
    private final MainView main;

    public DepartmentView(DepartmentController controller, MainView main) {
        this.controller = controller;
        this.main = main;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------------------------------------");
            System.out.println(  "|        QUAN LY PHONG BAN (HR)            |");
            System.out.println(  "|------------------------------------------|");
            System.out.println(  "|  1. Gan quan ly cho phong ban             |");
            System.out.println(  "|  2. Xem bao cao phong ban                |");
            System.out.println(  "|  0. Quay lai                             |");
            System.out.println(  "--------------------------------------------");
            int choice = main.readInt("Nhap lua chon: ");
            switch (choice) {
                case 1: assignManager(); break;
                case 2: viewReport();    break;
                case 0: back = true;     break;
                default: System.out.println("  [!] Lua chon khong hop le.");
            }
        }
    }

    private void assignManager() {
        String deptId    = main.readString("  Ma phong ban: ");
        String managerId = main.readString("  Ma quan ly moi: ");
        boolean ok = controller.assignManager(deptId, managerId);
        System.out.println(ok
            ? "  ✓ Da gan quan ly cho phong ban."
            : "  [!] Khong tim thay phong ban.");
    }

    private void viewReport() {
        List<Department> list = controller.getAll();
        if (list.isEmpty()) {
            System.out.println("  Chua co du lieu phong ban.");
            return;
        }
        System.out.printf("  %-8s %-20s %-10s %-12s %-15s%n",
            "Ma PB", "Ten phong ban", "Quan ly", "So NV", "Vi tri");
        System.out.println("  " + "-".repeat(70));
        for (Department d : list) {
            System.out.printf("  %-8s %-20s %-10s %-12d %-15s%n",
                d.getId(), d.getName(), d.getManagerId(),
                d.getTotalEmployees(), d.getLocation());
        }
    }
}
