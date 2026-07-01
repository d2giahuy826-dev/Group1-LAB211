import controller.AttendanceController;
import controller.AuthController;
import controller.DepartmentController;
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
 
        // ─── 1. Repository layer ─────────────────────────────────────────────
        EmployeeRepository     employeeRepo     = new EmployeeRepository();
        AttendanceRepository   attendanceRepo   = new AttendanceRepository();
        PayrollRunRepository   runRepo          = new PayrollRunRepository();
        PayrollEntryRepository entryRepo        = new PayrollEntryRepository("data/payroll_entries.csv");
        LeaveRequestRepository leaveRequestRepo = new LeaveRequestRepository();
        LeaveBalanceRepository leaveBalanceRepo = new LeaveBalanceRepository();
        UserRepository          userRepo        = new UserRepository();
 
        // ─── 2. Controller layer ─────────────────────────────────────────────
        PayrollController payrollController = new PayrollController(
                employeeRepo, attendanceRepo, entryRepo, runRepo, leaveRequestRepo);
 
        LeaveController leaveController = new LeaveController(
                leaveRequestRepo, leaveBalanceRepo);
 
        EmployeeController employeeController = new EmployeeController();
 
        AuthController authController = new AuthController(userRepo);
 
        DepartmentController departmentController = new DepartmentController();
 
        AttendanceController attendanceController = new AttendanceController();
 
        // ─── 3. View layer ───────────────────────────────────────────────────
        MainView mainView = new MainView(
                payrollController, leaveController, employeeController, authController,
                departmentController, attendanceController);
        mainView.start();
    }
}