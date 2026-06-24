package test;

import controller.PayrollController;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import repository.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test PayrollController.runPayroll() – NO_LOCK, đơn luồng.
 *
 * Chiến lược test:
 *  - Dùng @TempDir để redirect file CSV ra thư mục tạm → không đụng data thật
 *  - Tạo employees.csv nhỏ (3 nhân viên ACTIVE + 1 INACTIVE)
 *  - Tạo attendance.csv tương ứng
 *  - Tạo payroll_runs.csv trống
 *  - Tạo payroll_entries.csv trống
 *  - Gọi runPayroll(1, 2024) → kiểm tra số entry = số ACTIVE employee
 */
@DisplayName("PayrollController – NO_LOCK Tests")
class PayrollControllerTest {

    @TempDir
    Path tempDir;

    // File paths trong tempDir
    private Path employeesFile;
    private Path attendanceFile;
    private Path payrollRunsFile;
    private Path payrollEntriesFile;

    // Repositories trỏ vào tempDir
    private EmployeeRepository     employeeRepo;
    private AttendanceRepository   attendanceRepo;
    private PayrollEntryRepository payrollEntryRepo;
    private PayrollRunRepository   payrollRunRepo;

    private PayrollController controller;

    @BeforeEach
    void setUp() throws IOException {
        employeesFile      = tempDir.resolve("employees.csv");
        attendanceFile     = tempDir.resolve("attendance.csv");
        payrollRunsFile    = tempDir.resolve("payroll_runs.csv");
        payrollEntriesFile = tempDir.resolve("payroll_entries.csv");

        // ── Tạo employees.csv: 3 ACTIVE, 1 INACTIVE ──────────────────────────
        Files.writeString(employeesFile,
            "empId,fullName,deptId,empType,baseSalary,taxRate,joinDate,status\n" +
            "EMP001,Nguyen Van A,DEPT001,FULLTIME,12000000,0.1,2022-01-01,ACTIVE\n" +
            "EMP002,Tran Thi B,DEPT001,PARTTIME,8000000,0.05,2021-06-15,ACTIVE\n" +
            "EMP003,Le Van C,DEPT002,FULLTIME,15000000,0.1,2023-03-10,ACTIVE\n" +
            "EMP004,Pham Thi D,DEPT002,FULLTIME,10000000,0.1,2020-05-20,INACTIVE\n"
        );

        // ── Tạo attendance.csv: có dữ liệu cho EMP001 và EMP002, không có EMP003 ──
        Files.writeString(attendanceFile,
            "attendId,empId,month,year,workDays,absenceDays,overtimeHours,lateCount\n" +
            "ATT001,EMP001,1,2024,22,0,10,0\n" +   // đủ ngày, OT 10h
            "ATT002,EMP002,1,2024,20,2,0,1\n"       // nghỉ 2 ngày
            // EMP003 không có attendance → xử lý với 0 OT, 0 vắng
        );

        // ── Tạo payroll_runs.csv trống (chỉ có header) ───────────────────────
        Files.writeString(payrollRunsFile,
            "runId,month,year,triggeredBy,totalEmployees,totalNetPay,status,startedAt,completedAt\n"
        );

        // ── Tạo payroll_entries.csv trống (chỉ có header) ────────────────────
        Files.writeString(payrollEntriesFile,
            "entryId,empId,deptId,month,year,baseSalary,overtimePay,absenceDeduction," +
            "bonus,taxAmount,netSalary,status,version,processedAt\n"
        );

        // ── Khởi tạo repositories với anonymous subclass redirect tempDir ─────
        employeeRepo = new EmployeeRepository() {
            @Override protected String getFilePath() {
                return employeesFile.toString();
            }
        };

        // AttendanceRepository không expose getFilePath() override (không extend CsvRepository)
        // → Dùng helper để tạo một repository đọc trực tiếp từ file trong tempDir
        attendanceRepo = buildAttendanceRepo(attendanceFile.toString());

        payrollEntryRepo = new PayrollEntryRepository(payrollEntriesFile.toString());

        payrollRunRepo = new PayrollRunRepository() {
            @Override protected String getFilePath() {
                return payrollRunsFile.toString();
            }
        };

        controller = new PayrollController(
                employeeRepo, attendanceRepo, payrollEntryRepo, payrollRunRepo);
    }

    // ─── Helper: build AttendanceRepository trỏ vào tempDir ─────────────────

    /**
     * AttendanceRepository của Vy dùng `private final String FILE_PATH` instance
     * (không phải static) và không extend CsvRepository → không thể override
     * getFilePath(). Cách đơn giản nhất: tạo subclass set field qua constructor.
     *
     * Nếu AttendanceRepository chưa có constructor nhận path, dùng subclass mock đơn giản.
     */
    private AttendanceRepository buildAttendanceRepo(String path) {
        // AttendanceRepository hiện tại hardcode "data/attendance.csv"
        // → Subclass anonymous override findByEmployeeId và getAll để đọc từ tempDir
        return new AttendanceRepository() {
            @Override
            public java.util.List<model.AttendanceRecord> getAll() {
                java.util.List<model.AttendanceRecord> list = new java.util.ArrayList<>();
                try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(
                        java.nio.file.Paths.get(path))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        if (line.startsWith("attendId")) continue; // skip header
                        model.AttendanceRecord rec = new model.AttendanceRecord();
                        rec.fromCsvLine(line);
                        list.add(rec);
                    }
                } catch (java.io.IOException e) {
                    throw new UncheckedIOException(new java.io.IOException("Lỗi đọc attendance", e));
                }
                return list;
            }

            @Override
            public java.util.List<model.AttendanceRecord> findByEmployeeId(String employeeId) {
                return getAll().stream()
                        .filter(r -> employeeId.equals(r.getEmpId()))
                        .collect(java.util.stream.Collectors.toList());
            }
        };
    }

    // ─── TC01: Số entry = số ACTIVE employee ─────────────────────────────────

    @Test
    @DisplayName("TC01 – runPayroll(1/2024): số entry CSV = số ACTIVE employee (3)")
    void testEntryCountEqualsActiveEmployees() {
        PayrollController.RunResult result = controller.runPayroll(1, 2024);

        // 3 ACTIVE (EMP001, EMP002, EMP003), 1 INACTIVE (EMP004) → skip
        assertEquals(3, result.processedCount,
            "Phải tạo đúng 3 PayrollEntry cho 3 nhân viên ACTIVE");

        // Đếm thực tế trong CSV
        int csvCount = payrollEntryRepo.findAll().size();
        assertEquals(3, csvCount,
            "Số dòng trong payroll_entries.csv phải = 3");
    }

    // ─── TC02: PayrollRun được đánh dấu COMPLETED ────────────────────────────

    @Test
    @DisplayName("TC02 – PayrollRun sau runPayroll phải có status COMPLETED")
    void testPayrollRunCompleted() {
        controller.runPayroll(1, 2024);

        Optional<model.PayrollRun> run = payrollRunRepo.findByMonthAndYear(1, 2024);
        assertTrue(run.isPresent(), "Phải tìm thấy PayrollRun tháng 1/2024");
        assertTrue(run.get().isCompleted(), "PayrollRun phải có status COMPLETED");
        assertEquals(3, run.get().getTotalEmployees());
    }

    // ─── TC03: totalNetPay > 0 ────────────────────────────────────────────────

    @Test
    @DisplayName("TC03 – totalNetPay phải lớn hơn 0 sau khi chạy")
    void testTotalNetPayPositive() {
        PayrollController.RunResult result = controller.runPayroll(1, 2024);

        assertTrue(result.totalNetPay > 0,
            "totalNetPay phải > 0, nhưng nhận được: " + result.totalNetPay);
    }

    // ─── TC04: Chạy lại cùng tháng → ném IllegalStateException ──────────────

    @Test
    @DisplayName("TC04 – Chạy lại tháng đã COMPLETED → IllegalStateException")
    void testRunPayrollTwiceThrowsException() {
        controller.runPayroll(1, 2024);

        assertThrows(IllegalStateException.class,
            () -> controller.runPayroll(1, 2024),
            "Chạy lại tháng đã COMPLETED phải ném IllegalStateException");
    }

    // ─── TC05: Nhân viên INACTIVE không có entry ─────────────────────────────

    @Test
    @DisplayName("TC05 – Nhân viên INACTIVE (EMP004) không có PayrollEntry")
    void testInactiveEmployeeSkipped() {
        controller.runPayroll(1, 2024);

        boolean hasInactiveEntry = payrollEntryRepo.findAll().stream()
                .anyMatch(e -> e.getEmpId().equals("EMP004"));

        assertFalse(hasInactiveEntry,
            "EMP004 là INACTIVE → không được tạo PayrollEntry");
    }

    // ─── TC06: EMP001 có OT → overtimePay > 0 ───────────────────────────────

    @Test
    @DisplayName("TC06 – EMP001 có 10h OT → entry phải có overtimePay > 0")
    void testOvertimePayCalculated() {
        controller.runPayroll(1, 2024);

        model.PayrollEntry emp001Entry = payrollEntryRepo.findAll().stream()
                .filter(e -> e.getEmpId().equals("EMP001"))
                .findFirst()
                .orElse(null);

        assertNotNull(emp001Entry, "Phải có entry cho EMP001");
        assertTrue(emp001Entry.getOvertimePay() > 0,
            "EMP001 làm 10h OT → overtimePay phải > 0");
    }

    // ─── TC07: EMP002 nghỉ 2 ngày → absenceDeduction > 0 ────────────────────

    @Test
    @DisplayName("TC07 – EMP002 nghỉ 2 ngày → entry phải có absenceDeduction > 0")
    void testAbsenceDeductionCalculated() {
        controller.runPayroll(1, 2024);

        model.PayrollEntry emp002Entry = payrollEntryRepo.findAll().stream()
                .filter(e -> e.getEmpId().equals("EMP002"))
                .findFirst()
                .orElse(null);

        assertNotNull(emp002Entry, "Phải có entry cho EMP002");
        assertTrue(emp002Entry.getAbsenceDeduction() > 0,
            "EMP002 nghỉ 2 ngày → absenceDeduction phải > 0");
    }
}
