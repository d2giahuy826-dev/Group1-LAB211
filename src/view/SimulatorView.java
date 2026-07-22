package view;

import controller.SimulatorController;
import simulation.LockMechanism;
import simulation.SimulationResult;

import java.util.ArrayList;
import java.util.List;
import util.ConsoleTable;

public class SimulatorView {
    private final SimulatorController controller;
    private final MainView mainView;

    public SimulatorView(SimulatorController controller, MainView mainView) {
        this.controller = controller;
        this.mainView = mainView;
    }

    public void show() {
        printHeader("PAYROLL CONCURRENCY SIMULATOR");
        int threads = mainView.readInt("So thread HR (goi y 20): ");
        int departments = mainView.readInt("So phong ban (goi y 20): ");
        int employees = mainView.readInt("So nhan vien/phong (goi y 50): ");

        System.out.println();
        new ConsoleTable("STT", "CO CHE", "MO TA")
                .addRow(1, "NO_LOCK", "Khong khoa - co the trung thanh toan")
                .addRow(2, "SYNCHRONIZED", "Khoa monitor trong JVM")
                .addRow(3, "OPTIMISTIC", "Kiem tra entryId va version")
                .addRow(4, "FILE_LOCK", "Khoa file o tang he dieu hanh")
                .addRow(5, "COMPARE ALL", "Chay va so sanh ca 4 co che")
                .print();
        int choice = mainView.readInt("Chon co che: ");
        List<SimulationResult> results = new ArrayList<>();
        if (choice == 5) {
            for (LockMechanism mechanism : LockMechanism.values()) {
                System.out.printf("  [RUNNING] %-13s ...%n", mechanism);
                results.add(controller.runSimulation(threads, departments, employees, mechanism));
            }
        } else if (choice >= 1 && choice <= 4) {
            System.out.printf("  [RUNNING] %-13s ...%n", LockMechanism.values()[choice - 1]);
            results.add(controller.runSimulation(threads, departments, employees,
                    LockMechanism.values()[choice - 1]));
        } else {
            System.out.println("  [!] Lua chon khong hop le.");
            return;
        }
        printTable(results);
    }

    private void printTable(List<SimulationResult> results) {
        System.out.println();
        printHeader("SIMULATION RESULT");
        ConsoleTable table = new ConsoleTable(
                "MECHANISM", "SUCCESS", "DUPLICATE", "WRONG LEAVE", "TIME (ms)", "TPS", "STATUS");
        for (SimulationResult r : results) {
            String status = r.doublePayment == 0 && r.wrongLeave == 0 ? "PASS" : "ERROR";
            table.addRow(r.mechanism, format(r.success), format(r.doublePayment),
                    format(r.wrongLeave), format(r.elapsedMs), String.format("%.2f", r.tps), status);
        }
        table.print();
        System.out.println("Note: Detector reloaded the CSV files after all threads completed.");
    }

    private String format(long value) {
        return String.format("%,d", value);
    }

    private void printHeader(String title) {
        String line = "=".repeat(86);
        System.out.println(line);
        System.out.printf("%" + ((86 + title.length()) / 2) + "s%n", title);
        System.out.println(line);
    }
}
