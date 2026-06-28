import controller.EmployeeController;
import controller.LeaveController;
import controller.PayrollController;
import repository.*;
import view.MainView;

/**
 * Main – Entry point của ứng dụng Payroll Management System.
 *
 * Khởi tạo toàn bộ dependency theo thứ tự:
 *   Repository → Controller → View
 *
 * Chạy: javac -cp src src/Main.java && java -cp src Main
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Repository layer ──────────────────────────────────────────
        EmployeeRepository     employeeRepo   = new EmployeeRepository();
        AttendanceRepository   attendanceRepo = new AttendanceRepository();
        PayrollRunRepository   runRepo        = new PayrollRunRepository();

        // PayrollEntryRepository dùng constructor injection (path có thể override khi test)
        PayrollEntryRepository entryRepo = new PayrollEntryRepository("data/payroll_entries.csv");

        LeaveRequestRepository  leaveRequestRepo  = new LeaveRequestRepository();
        // LeaveBalanceRepository sẽ được Vương fix kế thừa CsvRepository — wire vào đây sau
        // LeaveBalanceRepository leaveBalanceRepo = new LeaveBalanceRepository();

        // ── 2. Controller layer ──────────────────────────────────────────
        PayrollController payrollController = new PayrollController(
            employeeRepo, attendanceRepo, entryRepo, runRepo
        );

        // LeaveController và EmployeeController — Duy/Vy implement, Huy wire vào đây
        // LeaveController    leaveController    = new LeaveController(leaveRequestRepo, leaveBalanceRepo);
        // EmployeeController employeeController = new EmployeeController(employeeRepo);

        // Placeholder để app compile được trong khi chờ Duy/Vy
        LeaveController    leaveController    = null; // TODO: Duy implement
        EmployeeController employeeController = null; // TODO: Vy implement

        // ── 3. View layer ────────────────────────────────────────────────
        MainView mainView = new MainView(payrollController, leaveController, employeeController);
        mainView.start();
    }
}
