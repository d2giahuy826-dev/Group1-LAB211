package controller;

import exception.DuplicatePaymentException;
import model.*;
import repository.AttendanceRepository;
import repository.EmployeeRepository;
import repository.PayrollEntryRepository;
import repository.PayrollRunRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * PayrollController – Điều phối nghiệp vụ tính lương hàng tháng.
 *
 * Tuần 5 – NO_LOCK: chạy đơn luồng, không có cơ chế đồng bộ nâng cao.
 * Luồng xử lý:
 *   1. Tìm PayrollRun PENDING cho tháng/năm yêu cầu (hoặc tạo mới)
 *   2. Load toàn bộ Employee ACTIVE
 *   3. Với mỗi nhân viên: load AttendanceRecord → tính lương → tạo PayrollEntry PENDING → save
 *   4. Cộng dồn totalNetPay
 *   5. Mark PayrollRun COMPLETED
 *
 * Cơ chế đồng bộ: NO_LOCK (đơn luồng, không có lock hay optimistic check)
 */
public class PayrollController {

    // ─── Dependencies ─────────────────────────────────────────────────────────

    private final EmployeeRepository      employeeRepo;
    private final AttendanceRepository    attendanceRepo;
    private final PayrollEntryRepository  payrollEntryRepo;
    private final PayrollRunRepository    payrollRunRepo;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Constructor mặc định – dùng đường dẫn file chuẩn.
     */
    public PayrollController() {
        this.employeeRepo     = new EmployeeRepository();
        this.attendanceRepo   = new AttendanceRepository();
        this.payrollEntryRepo = new PayrollEntryRepository("data/payroll_entries.csv");
        this.payrollRunRepo   = new PayrollRunRepository();
    }

    /**
     * Constructor injection – dùng cho test (truyền repo với đường dẫn tuỳ ý).
     */
    public PayrollController(EmployeeRepository employeeRepo,
                             AttendanceRepository attendanceRepo,
                             PayrollEntryRepository payrollEntryRepo,
                             PayrollRunRepository payrollRunRepo) {
        this.employeeRepo     = employeeRepo;
        this.attendanceRepo   = attendanceRepo;
        this.payrollEntryRepo = payrollEntryRepo;
        this.payrollRunRepo   = payrollRunRepo;
    }

    // ─── Main use case ────────────────────────────────────────────────────────

    /**
     * Chạy tính lương cho một tháng/năm cụ thể (NO_LOCK – đơn luồng).
     *
     * @param month tháng (1–12)
     * @param year  năm (ví dụ: 2024)
     * @return RunResult chứa số nhân viên đã xử lý và tổng lương thực nhận
     * @throws IllegalStateException nếu PayrollRun tháng này đã COMPLETED
     */
    public RunResult runPayroll(int month, int year) {
        // ── Bước 1: Tìm hoặc tạo PayrollRun ──────────────────────────────────
        PayrollRun run = resolvePayrollRun(month, year);

        String today = LocalDate.now().format(DATE_FMT);

        // ── Bước 2: Load toàn bộ Employee ACTIVE ─────────────────────────────
        List<Employee> employees = employeeRepo.loadAll();

        long totalNetPay    = 0L;
        int  processedCount = 0;
        int  skippedCount   = 0;

        // ── Bước 3: Xử lý từng nhân viên ─────────────────────────────────────
        for (Employee emp : employees) {
            // Chỉ tính lương cho nhân viên đang ACTIVE
            if (!emp.isActive()) {
                skippedCount++;
                continue;
            }

            // Bỏ qua nếu entry đã tồn tại (NO_LOCK: không cần optimistic check)
            String entryId = buildEntryId(emp.getId(), month, year);
            if (isAlreadyProcessed(entryId)) {
                skippedCount++;
                continue;
            }

            // Load dữ liệu chấm công tháng này
            AttendanceRecord attendance = findAttendance(emp.getId(), month, year);

            int overtimeHours = (attendance != null) ? attendance.getOvertimeHours() : 0;
            int absenceDays   = (attendance != null) ? attendance.getAbsenceDays()   : 0;

            // Tính lương qua SalaryCalculator
            SalaryCalculator calc = new SalaryCalculator(
                    emp.getBaseSalary(),
                    overtimeHours,
                    absenceDays,
                    emp.getTaxRate()
            ).calculate();

            // Tạo PayrollEntry PENDING
            PayrollEntry entry = buildEntry(
                    entryId, emp, month, year, calc, today);

            // Lưu vào CSV (NO_LOCK: gọi thẳng save, không wrap lock)
            try {
                payrollEntryRepo.save(entry);
            } catch (DuplicatePaymentException e) {
                // Race condition không xảy ra ở NO_LOCK, nhưng phòng thủ vẫn cần
                skippedCount++;
                continue;
            }

            totalNetPay += calc.getNetSalary();
            processedCount++;
        }

        // ── Bước 4: Cập nhật PayrollRun → COMPLETED ──────────────────────────
        run.setTotalEmployees(processedCount);
        run.setTotalNetPay(totalNetPay);
        run.markCompleted(today);
        payrollRunRepo.update(run);

        System.out.printf(
            "[PayrollController] runPayroll(%d/%d) → processed=%d, skipped=%d, totalNetPay=%,d%n",
            month, year, processedCount, skippedCount, totalNetPay);

        return new RunResult(run.getId(), processedCount, skippedCount, totalNetPay);
    }

    // ─── Helper: resolve PayrollRun ───────────────────────────────────────────

    /**
     * Tìm PayrollRun PENDING cho tháng/năm. Nếu chưa có thì tạo mới.
     * Nếu đã COMPLETED → ném exception.
     */
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

        // Tạo mới PayrollRun
        String runId  = "RUN_" + year + String.format("%02d", month);
        String today  = LocalDate.now().format(DATE_FMT);
        PayrollRun newRun = new PayrollRun(
                runId, month, year, "SYSTEM",
                0, 0L, RunStatus.PENDING, today, "");
        payrollRunRepo.add(newRun);
        return newRun;
    }

    // ─── Helper: build entryId ────────────────────────────────────────────────

    /**
     * Tạo entryId duy nhất theo format: PE_{empId}_{month}_{year}
     * Ví dụ: PE_EMP0001_1_2024
     */
    private String buildEntryId(String empId, int month, int year) {
        return "PE_" + empId + "_" + month + "_" + year;
    }

    // ─── Helper: check duplicate ──────────────────────────────────────────────

    private boolean isAlreadyProcessed(String entryId) {
        return payrollEntryRepo.findById(entryId) != null;
    }

    // ─── Helper: find attendance ──────────────────────────────────────────────

    /**
     * Tìm AttendanceRecord của nhân viên trong tháng/năm cụ thể.
     * Trả về null nếu không có dữ liệu chấm công (sẽ tính với 0 OT, 0 vắng).
     */
    private AttendanceRecord findAttendance(String empId, int month, int year) {
        return attendanceRepo.findByEmployeeId(empId).stream()
                .filter(a -> a.getMonth() == month && a.getYear() == year)
                .findFirst()
                .orElse(null);
    }

    // ─── Helper: build PayrollEntry ───────────────────────────────────────────

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

    // ─── Result DTO ───────────────────────────────────────────────────────────

    /**
     * Kết quả trả về của runPayroll().
     */
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
