package controller;

import exception.DuplicatePaymentException;
import model.*;
import repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PayrollController – Điều phối luồng xử lý bảng lương hàng tháng.
 *
 * Tuần 5: NO_LOCK (đơn luồng) — chưa có synchronized/optimistic.
 * Tuần 7: thêm runPayrollWithFileLock(), runPayrollWithOptimistic().
 *
 * ⚠ KHÔNG chứa công thức tính lương — toàn bộ nằm trong SalaryCalculator (Model).
 * ⚠ KHÔNG ghi CSV trực tiếp — toàn bộ qua Repository.
 */
public class PayrollController {

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final EmployeeRepository      employeeRepo;
    private final AttendanceRepository    attendanceRepo;
    private final PayrollEntryRepository  entryRepo;
    private final PayrollRunRepository    runRepo;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public PayrollController(EmployeeRepository employeeRepo,
                             AttendanceRepository attendanceRepo,
                             PayrollEntryRepository entryRepo,
                             PayrollRunRepository runRepo) {
        this.employeeRepo   = employeeRepo;
        this.attendanceRepo = attendanceRepo;
        this.entryRepo      = entryRepo;
        this.runRepo        = runRepo;
    }

    // ─── NO_LOCK: Chạy bảng lương đơn luồng ─────────────────────────────────

    /**
     * Chạy bảng lương tháng/năm theo cơ chế NO_LOCK (đơn luồng).
     *
     * Quy trình:
     *  1. Kiểm tra PayrollRun tháng này đã tồn tại chưa
     *  2. Tạo PayrollRun mới với status PENDING
     *  3. Load toàn bộ nhân viên ACTIVE
     *  4. Với từng nhân viên: load attendance → tính lương → tạo PayrollEntry
     *  5. Cộng dồn totalNetPay → mark PayrollRun COMPLETED
     *
     * @param month tháng cần chạy (1–12)
     * @param year  năm cần chạy
     * @return PayrollRun đã hoàn thành
     * @throws IllegalStateException nếu tháng này đã có PayrollRun
     */
    public PayrollRun runPayroll(int month, int year) {
        // 1. Kiểm tra trùng
        Optional<PayrollRun> existing = runRepo.findByMonthAndYear(month, year);
        if (existing.isPresent()) {
            throw new IllegalStateException(
                "Đã tồn tại PayrollRun cho tháng " + month + "/" + year
                + ". Status: " + existing.get().getStatus());
        }

        // 2. Tạo PayrollRun mới
        String runId    = "RUN_" + year + String.format("%02d", month);
        String startedAt = LocalDate.now().toString();

        PayrollRun run = new PayrollRun(
            runId, month, year,
            "SYSTEM",        // triggeredBy
            0, 0L,           // totalEmployees, totalNetPay — cập nhật sau
            RunStatus.PENDING,
            startedAt, ""
        );
        runRepo.add(run);

        // 3. Load nhân viên ACTIVE
        List<Employee> employees = employeeRepo.loadAll();
        List<Employee> activeEmployees = new ArrayList<>();
        for (Employee emp : employees) {
            if (emp.isActive()) {
                activeEmployees.add(emp);
            }
        }

        // 4. Tính lương từng nhân viên
        long totalNetPay = 0L;
        int  processed   = 0;

        for (Employee emp : activeEmployees) {
            // Kiểm tra entry đã tồn tại chưa (tránh double payment)
            String entryId = buildEntryId(emp.getId(), month, year);
            PayrollEntry existing_entry = entryRepo.findById(entryId);
            if (existing_entry != null && existing_entry.isProcessed()) {
                System.out.println("SKIP (đã xử lý): " + entryId);
                continue;
            }

            // Load attendance tháng này
            AttendanceRecord attendance = findAttendance(emp.getId(), month, year);
            int overtimeHours = attendance != null ? attendance.getOvertimeHours() : 0;
            int absenceDays   = attendance != null ? attendance.getAbsenceDays()   : 0;

            // Tính lương — toàn bộ công thức nằm trong SalaryCalculator
            SalaryCalculator calc = new SalaryCalculator(
                emp.getBaseSalary(),
                overtimeHours,
                absenceDays,
                emp.getTaxRate()
            );
            calc.calculate();

            // Tạo PayrollEntry
            PayrollEntry entry = buildPayrollEntry(
                entryId, emp, month, year, calc
            );

            // Lưu entry
            try {
                entryRepo.save(entry);
                processed++;
                totalNetPay += entry.getNetSalary();
            } catch (DuplicatePaymentException e) {
                System.err.println("WARN Double Payment ngăn được: " + entryId);
            }
        }

        // 5. Mark PayrollRun COMPLETED
        run.setTotalEmployees(processed);
        run.setTotalNetPay(totalNetPay);
        run.markCompleted(LocalDate.now().toString());
        runRepo.update(run);

        System.out.printf("[PayrollController] Hoàn thành: %d/%d nhân viên | Tổng: %,d VNĐ%n",
            processed, activeEmployees.size(), totalNetPay);

        return run;
    }

    // ─── Query helpers (dùng cho View) ───────────────────────────────────────

    /**
     * Lấy tất cả PayrollEntry của một tháng/năm.
     * PayrollView gọi method này để hiển thị bảng lương.
     */
    public List<PayrollEntry> getEntriesByMonthYear(int month, int year) {
        List<PayrollEntry> result = new ArrayList<>();
        for (PayrollEntry e : entryRepo.findAll()) {
            if (e.getMonth() == month && e.getYear() == year) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Lấy PayrollEntry của 1 nhân viên trong 1 tháng.
     */
    public PayrollEntry getEntryByEmpAndMonth(String empId, int month, int year) {
        return entryRepo.findById(buildEntryId(empId, month, year));
    }

    /**
     * Lấy lịch sử PayrollRun (dùng cho ReportView).
     */
    public List<PayrollRun> getAllRuns() {
        return runRepo.loadAll();
    }

    /**
     * Lấy PayrollRun mới nhất.
     */
    public Optional<PayrollRun> getLatestRun() {
        return runRepo.findLatest();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Tìm AttendanceRecord của nhân viên trong tháng/năm cụ thể.
     */
    private AttendanceRecord findAttendance(String empId, int month, int year) {
        List<AttendanceRecord> records = attendanceRepo.findByEmployeeId(empId);
        for (AttendanceRecord r : records) {
            if (r.getMonth() == month && r.getYear() == year) {
                return r;
            }
        }
        return null; // Không có dữ liệu chấm công → dùng mặc định
    }

    /**
     * Build entry ID theo quy tắc nhất quán: PE_{empId}_{year}{month:02d}
     */
    private String buildEntryId(String empId, int month, int year) {
        return "PE_" + empId + "_" + year + String.format("%02d", month);
    }

    /**
     * Tạo PayrollEntry từ kết quả SalaryCalculator.
     * ⚠ KHÔNG chứa công thức — chỉ đọc kết quả từ calc.
     */
    private PayrollEntry buildPayrollEntry(String entryId, Employee emp,
                                           int month, int year,
                                           SalaryCalculator calc) {
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
        entry.setStatus(PayStatus.PROCESSED);
        entry.setVersion(0);
        entry.setProcessedAt(LocalDate.now().toString());
        return entry;
    }
}
