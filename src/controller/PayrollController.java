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
import java.time.YearMonth;
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

    /**
     * Chay bang luong binh thuong (khong force).
     * Neu run cho thang/nam nay da COMPLETED -> throw IllegalStateException.
     */
    public RunResult runPayroll(int month, int year) {
        return runPayroll(month, year, false);
    }

    /**
     * Chay bang luong voi tuy chon force.
     *
     * @param force neu true:
     *              - Run da COMPLETED se duoc "mo lai" (PENDING) thay vi bi chan.
     *              - Entry da ton tai cho nhan vien/thang/nam do se duoc TINH LAI
     *                va UPDATE tai cho (giu nguyen entryId, tang version),
     *                thay vi bi bo qua (skip) nhu truoc.
     *              neu false: giu nguyen hanh vi cu (run COMPLETED bi chan,
     *              entry da ton tai bi skip).
     */
    public RunResult runPayroll(int month, int year, boolean force) {
        PayrollRun run = resolvePayrollRun(month, year, force);
        String today = LocalDate.now().format(DATE_FMT);

        List<Employee> employees = employeeRepo.loadAll();

        long totalNetPay    = 0L;
        int  processedCount = 0; // entry moi (chua tung ton tai)
        int  updatedCount   = 0; // entry cu duoc cap nhat lai (chi khi force = true)
        int  skippedCount   = 0;

        for (Employee emp : employees) {
            if (!emp.isActive()) {
                skippedCount++;
                continue;
            }

            String entryId = buildEntryId(emp.getId(), month, year);
            PayrollEntry existing = payrollEntryRepo.findById(entryId);

            if (existing != null && !force) {
                // Da co entry va khong force -> giu hanh vi cu: bo qua
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

            if (existing != null) {
                // force = true va da co entry cu -> UPDATE tai cho, giu lich su ID,
                // tang version de biet entry nay da duoc tinh lai bao nhieu lan.
                entry.setVersion(existing.getVersion() + 1);
                payrollEntryRepo.update(entry);
                updatedCount++;
            } else {
                try {
                    payrollEntryRepo.save(entry);
                } catch (DuplicatePaymentException e) {
                    skippedCount++;
                    continue;
                }
                processedCount++;
            }

            totalNetPay += calc.getNetSalary();
        }

        run.setTotalEmployees(processedCount + updatedCount);
        run.setTotalNetPay(totalNetPay);
        run.markCompleted(today);
        payrollRunRepo.update(run);

        System.out.printf(
            "[PayrollController] runPayroll(%d/%d, force=%b) -> moi=%d, capNhat=%d, boQua=%d, totalNetPay=%,d%n",
            month, year, force, processedCount, updatedCount, skippedCount, totalNetPay);

        return new RunResult(run.getId(), processedCount + updatedCount, skippedCount, totalNetPay);
    }

    // ─── Query methods cho PayrollView ───────────────────────────────────────

    public List<PayrollEntry> getEntriesByMonthYear(int month, int year) {
        return payrollEntryRepo.findAll().stream()
                .filter(e -> e.getMonth() == month && e.getYear() == year)
                .map(this::refreshStatus)
                .collect(Collectors.toList());
    }

    public PayrollEntry getEntryByEmpAndMonth(String empId, int month, int year) {
        PayrollEntry entry = payrollEntryRepo.findAll().stream()
                .filter(e -> e.getEmpId().equals(empId)
                        && e.getMonth() == month
                        && e.getYear() == year)
                .findFirst()
                .orElse(null);
        return refreshStatus(entry);
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

    /**
     * Tim hoac tao PayrollRun cho thang/nam.
     *
     * @param force neu true va run da COMPLETED, se "mo lai" run ve PENDING
     *              thay vi throw exception, cho phep chay lai.
     */
    private PayrollRun resolvePayrollRun(int month, int year, boolean force) {
        Optional<PayrollRun> existing = payrollRunRepo.findByMonthAndYear(month, year);

        if (existing.isPresent()) {
            PayrollRun run = existing.get();
            if (run.isCompleted()) {
                if (!force) {
                    throw new IllegalStateException(
                        "PayrollRun cho tháng " + month + "/" + year
                        + " đã COMPLETED. Không thể chạy lại.");
                }
                // force = true: mo lai run de cho phep chay lai / cap nhat
                run.setStatus(RunStatus.PENDING);
                run.setCompletedAt("");
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
        entry.setVersion(0);

        // ── Trang thai PENDING / PROCESSED dua tren thoi gian thuc ──────────
        // Neu ngay hien tai da den/qua ngay cuoi cung cua thang luong nay
        // (vd: chay luong thang 7/2026, hom nay >= 31/07/2026) → PROCESSED.
        // Neu chua den ngay cuoi thang → PENDING.
        if (isReachedEndOfMonth(month, year)) {
            entry.setStatus(PayStatus.PROCESSED);
            entry.setProcessedAt(today);
        } else {
            entry.setStatus(PayStatus.PENDING);
            entry.setProcessedAt("");
        }

        return entry;
    }

    /**
     * Kiem tra ngay hien tai (LocalDate.now()) da den hoac qua ngay cuoi cung
     * cua thang/nam luong duoc truyen vao hay chua. Tu dong xu ly dung cho
     * moi thang (28/29/30/31 ngay) nho YearMonth.atEndOfMonth().
     */
    private boolean isReachedEndOfMonth(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate endOfMonth = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();
        return !today.isBefore(endOfMonth); // today >= endOfMonth
    }

    /**
     * Cap nhat lai trang thai cua mot PayrollEntry theo thoi gian thuc moi khi
     * duoc truy van (xem bang luong / xem chi tiet). Dieu nay dam bao entry
     * duoc tao dau thang (con PENDING) se tu dong hien PROCESSED khi da den
     * ngay cuoi thang, ke ca khi khong chay lai payroll.
     *
     * Luu y: chi cap nhat trong bo nho de hien thi; neu muon ghi de xuong
     * file CSV, can bo sung method update(entry) trong PayrollEntryRepository
     * va goi no o day.
     */
    private PayrollEntry refreshStatus(PayrollEntry entry) {
        if (entry == null) return null;

        if (isReachedEndOfMonth(entry.getMonth(), entry.getYear())
                && entry.getStatus() != PayStatus.PROCESSED) {
            String processedDate = LocalDate.now().format(DATE_FMT);
            entry.markProcessed(processedDate);
        }

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