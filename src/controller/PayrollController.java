package controller;

import exception.DuplicatePaymentException;
import model.*;
import repository.AttendanceRepository;
import repository.EmployeeRepository;
import repository.LeaveRequestRepository;
import repository.PayrollEntryRepository;
import repository.PayrollRunRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PayrollController {

    private final EmployeeRepository      employeeRepo;
    private final AttendanceRepository    attendanceRepo;
    private final PayrollEntryRepository  payrollEntryRepo;
    private final PayrollRunRepository    payrollRunRepo;
    private final LeaveRequestRepository  leaveRequestRepo; // thêm để cấp dữ liệu cho ReportView

    private final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── Constructor mặc định (tự tạo repo) ──────────────────────────────────
    public PayrollController() {
        this.employeeRepo     = new EmployeeRepository();
        this.attendanceRepo   = new AttendanceRepository();
        this.payrollEntryRepo = new PayrollEntryRepository("data/payroll_entries.csv");
        this.payrollRunRepo   = new PayrollRunRepository();
        this.leaveRequestRepo = new LeaveRequestRepository();
    }

    // ─── Constructor DI (dùng trong Main.java và Test) ────────────────────────
    public PayrollController(EmployeeRepository employeeRepo,
                             AttendanceRepository attendanceRepo,
                             PayrollEntryRepository payrollEntryRepo,
                             PayrollRunRepository payrollRunRepo,
                             LeaveRequestRepository leaveRequestRepo) {
        this.employeeRepo     = employeeRepo;
        this.attendanceRepo   = attendanceRepo;
        this.payrollEntryRepo = payrollEntryRepo;
        this.payrollRunRepo   = payrollRunRepo;
        this.leaveRequestRepo = leaveRequestRepo;
    }

    // ─── Main use case ────────────────────────────────────────────────────────

    public RunResult runPayroll(int month, int year) {
        PayrollRun run = resolvePayrollRun(month, year);
        String today = LocalDate.now().format(DATE_FMT);

        List<Employee> employees = employeeRepo.loadAll();

        long totalNetPay    = 0L;
        int  processedCount = 0;
        int  skippedCount   = 0;

        for (Employee emp : employees) {
            if (!emp.isActive()) {
                skippedCount++;
                continue;
            }

            String entryId = buildEntryId(emp.getId(), month, year);
            if (isAlreadyProcessed(entryId)) {
                skippedCount++;
                continue;
            }

            AttendanceRecord attendance = findAttendance(emp.getId(), month, year);

            int overtimeHours = (attendance != null) ? attendance.getOvertimeHours() : 0;
            int absenceDays   = (attendance != null) ? attendance.getAbsenceDays()   : 0;

            SalaryCalculator calc = new SalaryCalculator(
                    emp.getBaseSalary(),
                    overtimeHours,
                    absenceDays,
                    emp.getTaxRate()
            ).calculate();

            PayrollEntry entry = buildEntry(entryId, emp, month, year, calc, today);

            try {
                payrollEntryRepo.save(entry);
            } catch (DuplicatePaymentException e) {
                skippedCount++;
                continue;
            }

            totalNetPay += calc.getNetSalary();
            processedCount++;
        }

        run.setTotalEmployees(processedCount);
        run.setTotalNetPay(totalNetPay);
        run.markCompleted(today);
        payrollRunRepo.update(run);

        System.out.printf(
            "[PayrollController] runPayroll(%d/%d) → processed=%d, skipped=%d, totalNetPay=%,d%n",
            month, year, processedCount, skippedCount, totalNetPay);

        return new RunResult(run.getId(), processedCount, skippedCount, totalNetPay);
    }

    // ─── Query methods cho PayrollView ───────────────────────────────────────

    public List<PayrollEntry> getEntriesByMonthYear(int month, int year) {
        return payrollEntryRepo.findAll().stream()
                .filter(e -> e.getMonth() == month && e.getYear() == year)
                .collect(Collectors.toList());
    }

    public PayrollEntry getEntryByEmpAndMonth(String empId, int month, int year) {
        return payrollEntryRepo.findAll().stream()
                .filter(e -> e.getEmpId().equals(empId)
                        && e.getMonth() == month
                        && e.getYear() == year)
                .findFirst()
                .orElse(null);
    }

    public List<PayrollRun> getAllRuns() {
        return payrollRunRepo.loadAll();
    }

    // ─── Query methods cho ReportView (đúng chuẩn MVC — View không gọi repo) ──

    public List<Employee> getAllEmployees() {
        return employeeRepo.loadAll();
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepo.loadAll();
    }

    // ─── Conduct Payroll Audit ────────────────────────────────────────────────

    /**
     * Kiểm toán bảng lương: tìm các entry bất thường (netSalary âm).
     * Có thể mở rộng thêm quy tắc kiểm toán khác trong tương lai.
     */
    public List<PayrollEntry> auditNegativeNetSalary() {
        return payrollEntryRepo.findAll().stream()
                .filter(e -> e.getNetSalary() < 0)
                .collect(Collectors.toList());
    }

    // ─── Export Payroll CSV ───────────────────────────────────────────────────

    /**
     * Xuất bảng lương của một tháng ra file CSV chỉ định.
     *
     * @return đường dẫn file đã ghi
     */
    public String exportPayrollCsv(int month, int year, String outputPath) {
        List<PayrollEntry> entries = getEntriesByMonthYear(month, year);

        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            pw.println("entryId,empId,deptId,month,year,baseSalary,overtimePay,"
                    + "absenceDeduction,bonus,taxAmount,netSalary,status,version,processedAt");
            for (PayrollEntry e : entries) {
                pw.println(e.toCsvLine());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Khong the ghi file: " + ex.getMessage());
        }

        return outputPath;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private PayrollRun resolvePayrollRun(int month, int year) {
        Optional<PayrollRun> existing = payrollRunRepo.findByMonthAndYear(month, year);

        if (existing.isPresent()) {
            PayrollRun run = existing.get();
            if (run.isCompleted()) {
                throw new IllegalStateException(
                    "PayrollRun cho tháng " + month + "/" + year
                    + " đã COMPLETED. Không thể chạy lại.");
            }
            return run;
        }

        String runId = "RUN_" + year + String.format("%02d", month);
        String today = LocalDate.now().format(DATE_FMT);
        PayrollRun newRun = new PayrollRun(
                runId, month, year, "SYSTEM",
                0, 0L, RunStatus.PENDING, today, "");
        payrollRunRepo.add(newRun);
        return newRun;
    }

    private String buildEntryId(String empId, int month, int year) {
        return "PE_" + empId + "_" + month + "_" + year;
    }

    private boolean isAlreadyProcessed(String entryId) {
        return payrollEntryRepo.findById(entryId) != null;
    }

    private AttendanceRecord findAttendance(String empId, int month, int year) {
        return attendanceRepo.findByEmployeeId(empId).stream()
                .filter(a -> a.getMonth() == month && a.getYear() == year)
                .findFirst()
                .orElse(null);
    }

    private PayrollEntry buildEntry(String entryId, Employee emp,
                                    int month, int year,
                                    SalaryCalculator calc,
                                    String today) {
        PayrollEntry entry = new PayrollEntry();
        entry.setId(entryId);
        entry.setEmpId(emp.getId());
        entry.setDeptId(emp.getDeptId());
        entry.setMonth(month);
        entry.setYear(year);
        entry.setBaseSalary((long) emp.getBaseSalary());
        entry.setOvertimePay((long) calc.getOvertimePay());
        entry.setAbsenceDeduction((long) calc.getAbsenceDeduction());
        entry.setBonus((long) calc.getAttendanceBonus());
        entry.setTaxAmount((long) calc.getTaxAmount());
        entry.setNetSalary((long) calc.getNetSalary());
        entry.setStatus(PayStatus.PENDING);
        entry.setVersion(0);
        entry.setProcessedAt(today);
        return entry;
    }

    // ─── RunResult DTO ────────────────────────────────────────────────────────

    public static class RunResult {
        public final String runId;
        public final int    processedCount;
        public final int    skippedCount;
        public final long   totalNetPay;

        public RunResult(String runId, int processedCount,
                         int skippedCount, long totalNetPay) {
            this.runId          = runId;
            this.processedCount = processedCount;
            this.skippedCount   = skippedCount;
            this.totalNetPay    = totalNetPay;
        }

        @Override
        public String toString() {
            return String.format(
                "RunResult{runId='%s', processed=%d, skipped=%d, totalNetPay=%,d}",
                runId, processedCount, skippedCount, totalNetPay);
        }
    }
}