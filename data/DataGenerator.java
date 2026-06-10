

import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DataGenerator.java
 * Sinh dữ liệu giả cho 7 file CSV của hệ thống Payroll.
 *
 * Thứ tự sinh (theo FK dependency):
 *   1. departments.csv       (≥ 20 dong)
 *   2. employees.csv         (≥ 1,000 dong)
 *   3. leave_balances.csv    (= số employees, version = 0)
 *   4. leave_requests.csv    (≥ 4,000 dong)
 *   5. attendance.csv        (≥ 12,000 dong — 12 tháng × employees)
 *   6. payroll_entries.csv   (≥ 12,000 dong — 12 tháng × employees)
 *   7. payroll_runs.csv      (≥ 12 dong — 1 run/tháng)
 *
 * Tổng ≥ 29,000 dong.
 * Usage: java data.DataGenerator [output_dir]
 */
public class DataGenerator {

    // ─── Cấu hình số lượng ───────────────────────────────────────────────────
    static final int NUM_DEPARTMENTS  = 20;
    static final int NUM_EMPLOYEES    = 1_000;
    static final int NUM_MONTHS       = 12;       // 12 tháng trong năm
    static final int LEAVE_REQ_TARGET = 4_000;    // tổng đơn nghỉ phép
    static final int PAYROLL_YEAR     = 2024;

    // ─── Dữ liệu mẫu ─────────────────────────────────────────────────────────
    static final String[] DEPT_NAMES = {
        "Engineering","Marketing","Finance","HR","Sales",
        "Operations","Legal","R&D","IT Support","Customer Service",
        "Procurement","Logistics","Quality Assurance","Product","Design",
        "Business Development","Data Analytics","Security","Compliance","Training"
    };

    static final String[] FIRST_NAMES = {
        "Minh","Huy","Linh","Trang","Nam","Anh","Khoa","Phúc",
        "Thảo","Dũng","Bảo","Hà","Long","Mai","Tuấn","Hương",
        "Đức","Ngọc","Quân","Lan","Việt","Thư","Khánh","Nhung"
    };

    static final String[] LAST_NAMES = {
        "Nguyễn","Trần","Lê","Phạm","Hoàng","Huỳnh","Phan","Vũ",
        "Võ","Đặng","Bùi","Đỗ","Hồ","Ngô","Dương","Lý","Đinh","Trịnh"
    };

    static final String[] EMP_TYPES      = {"FULLTIME", "PARTTIME"};
    static final String[] LEAVE_TYPES    = {"ANNUAL", "SICK"};
    static final String[] LEAVE_STATUSES = {"PENDING", "APPROVED", "REJECTED"};
    static final String[] PAY_STATUSES   = {"PENDING", "PROCESSED"};
    static final String[] RUN_STATUSES   = {"COMPLETED"};

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── State ────────────────────────────────────────────────────────────────
    static final Random RNG = new Random(42); // seed cố định → reproducible
    static String outputDir = "data";

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        if (args.length > 0) outputDir = args[0];
        Files.createDirectories(Paths.get(outputDir));

        System.out.println("=== LAB211 DataGenerator ===");
        System.out.println("Output: " + Paths.get(outputDir).toAbsolutePath());
        System.out.println();

        long t0 = System.currentTimeMillis();

        List<String> deptIds = generateDepartments();
        List<String> empIds  = generateEmployees(deptIds);
        generateLeaveBalances(empIds);
        generateLeaveRequests(empIds);
        generateAttendance(empIds);
        generatePayrollEntries(empIds, deptIds);
        generatePayrollRuns();

        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("%n✅ Hoàn thành trong %d ms%n", elapsed);
        System.out.println("Kiểm tra thư mục: " + outputDir);
    }

    // ─── 1. departments.csv ───────────────────────────────────────────────────
    static List<String> generateDepartments() throws IOException {
        List<String> ids = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"deptId","deptName","managerId","location","headCount"});

        for (int i = 0; i < NUM_DEPARTMENTS; i++) {
            String id = String.format("DEPT%03d", i + 1);
            ids.add(id);
            rows.add(new String[]{
                id,
                DEPT_NAMES[i],
                String.format("EMP%04d", RNG.nextInt(50) + 1), // placeholder managerId
                "Floor " + (RNG.nextInt(10) + 1),
                String.valueOf(RNG.nextInt(91) + 10)            // 10–100
            });
        }
        writeCsv("departments.csv", rows);
        return ids;
    }

    // ─── 2. employees.csv ─────────────────────────────────────────────────────
    static List<String> generateEmployees(List<String> deptIds) throws IOException {
        List<String> ids = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "empId","fullName","deptId","empType",
            "baseSalary","taxRate","joinDate","status"
        });

        for (int i = 0; i < NUM_EMPLOYEES; i++) {
            String id = String.format("EMP%04d", i + 1);
            ids.add(id);

            String name = LAST_NAMES[RNG.nextInt(LAST_NAMES.length)]
                        + " " + FIRST_NAMES[RNG.nextInt(FIRST_NAMES.length)];
            name = removeAccents(name);
            String deptId  = deptIds.get(i % deptIds.size());
            String type    = EMP_TYPES[RNG.nextInt(EMP_TYPES.length)];
            double base    = type.equals("FULLTIME")
                           ? 8_000_000 + RNG.nextInt(12_000_000)   // 8–20 triệu
                           : 4_000_000 + RNG.nextInt(6_000_000);   // 4–10 triệu
            double tax     = type.equals("FULLTIME") ? 0.10 : 0.05;
            LocalDate join = LocalDate.of(2019, 1, 1)
                               .plusDays(RNG.nextInt(365 * 5));

            rows.add(new String[]{
                id, name, deptId, type,
                String.valueOf((long) base),
                String.valueOf(tax),
                join.format(DATE_FMT),
                "ACTIVE"
            });
        }
        writeCsv("employees.csv", rows);
        return ids;
    }

    // ─── 3. leave_balances.csv ───────────────────────────────────────────────
    static void generateLeaveBalances(List<String> empIds) throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "balanceId","empId","year",
            "annualTotal","annualUsed","annualRemaining",
            "sickTotal","sickUsed","sickRemaining",
            "version"                           // ← CRITICAL: Optimistic Lock
        });

        for (int i = 0; i < empIds.size(); i++) {
            int annualUsed = RNG.nextInt(13);   // 0–12
            int sickUsed   = RNG.nextInt(7);    // 0–6
            rows.add(new String[]{
                String.format("LB%05d", i + 1),
                empIds.get(i),
                String.valueOf(PAYROLL_YEAR),
                "12", String.valueOf(annualUsed), String.valueOf(12 - annualUsed),
                "6",  String.valueOf(sickUsed),   String.valueOf(6  - sickUsed),
                "0"                              // version bắt đầu từ 0
            });
        }
        writeCsv("leave_balances.csv", rows);
    }

    // ─── 4. leave_requests.csv ───────────────────────────────────────────────
    static void generateLeaveRequests(List<String> empIds) throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "requestId","empId","leaveType","startDate","endDate",
            "days","reason","status","approvedBy","createdAt"
        });

        int count = 0;
        while (count < LEAVE_REQ_TARGET) {
            String empId     = empIds.get(RNG.nextInt(empIds.size()));
            String leaveType = LEAVE_TYPES[RNG.nextInt(LEAVE_TYPES.length)];
            int month        = RNG.nextInt(12) + 1;
            int day          = RNG.nextInt(20) + 1;
            LocalDate start  = LocalDate.of(PAYROLL_YEAR, month, day);
            int days         = RNG.nextInt(5) + 1; // 1–5 ngày
            LocalDate end    = start.plusDays(days - 1);
            String status    = LEAVE_STATUSES[RNG.nextInt(LEAVE_STATUSES.length)];
            String approver  = status.equals("PENDING") ? ""
                             : String.format("EMP%04d", RNG.nextInt(50) + 1);

            rows.add(new String[]{
                String.format("LR%06d", count + 1),
                empId, leaveType,
                start.format(DATE_FMT), end.format(DATE_FMT),
                String.valueOf(days),
                "Nghỉ phép tháng " + month,
                status, approver,
                LocalDate.of(PAYROLL_YEAR, month, 1).format(DATE_FMT)
            });
            count++;
        }
        writeCsv("leave_requests.csv", rows);
    }

    // ─── 5. attendance.csv ───────────────────────────────────────────────────
    static void generateAttendance(List<String> empIds) throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "attendId","empId","month","year",
            "workDays","absenceDays","overtimeHours","lateCount"
        });

        int count = 0;
        for (String empId : empIds) {
            for (int month = 1; month <= NUM_MONTHS; month++) {
                int workDays     = 22 - RNG.nextInt(5);     // 17–22 ngày
                int absenceDays  = RNG.nextInt(4);           // 0–3 ngày
                int overtimeHrs  = RNG.nextInt(21);          // 0–20 giờ
                int lateCount    = RNG.nextInt(4);           // 0–3 lần

                rows.add(new String[]{
                    String.format("ATT%07d", ++count),
                    empId,
                    String.valueOf(month),
                    String.valueOf(PAYROLL_YEAR),
                    String.valueOf(workDays),
                    String.valueOf(absenceDays),
                    String.valueOf(overtimeHrs),
                    String.valueOf(lateCount)
                });
            }
        }
        writeCsv("attendance.csv", rows);
    }

    // ─── 6. payroll_entries.csv ──────────────────────────────────────────────
    static void generatePayrollEntries(List<String> empIds, List<String> deptIds)
            throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "entryId","empId","deptId","month","year",
            "baseSalary","overtimePay","absenceDeduction",
            "bonus","taxAmount","netSalary",
            "status","version","processedAt"    // ← version: Optimistic Lock
        });

        // Đọc lại employees để lấy baseSalary & taxRate (đơn giản: random lại theo seed)
        Random r2 = new Random(42); // cùng seed → cùng giá trị với generateEmployees
        long[] baseSalaries = new long[empIds.size()];
        double[] taxRates   = new double[empIds.size()];
        for (int i = 0; i < empIds.size(); i++) {
            boolean fulltime = (i % 3 != 0); // mock: PARTTIME mỗi 3 người
            baseSalaries[i] = fulltime
                ? 8_000_000L + r2.nextInt(12_000_000)
                : 4_000_000L + r2.nextInt(6_000_000);
            taxRates[i] = fulltime ? 0.10 : 0.05;
        }

        int count = 0;
        for (int ei = 0; ei < empIds.size(); ei++) {
            String deptId = deptIds.get(ei % deptIds.size());
            for (int month = 1; month <= NUM_MONTHS; month++) {
                long   base       = baseSalaries[ei];
                int    otHours    = RNG.nextInt(21);
                long   otPay      = Math.round(base / 22.0 / 8 * otHours * 1.5);
                int    abseDays   = RNG.nextInt(4);
                long   abseDed    = Math.round(base / 22.0 * abseDays);
                long   bonus      = (abseDays == 0) ? Math.round(base * 0.05) : 0;
                long   gross      = base + otPay - abseDed + bonus;
                long   tax        = Math.round(gross * taxRates[ei]);
                long   net        = gross - tax;

                // Tháng cuối để PENDING để Simulator xử lý
                String status     = (month == NUM_MONTHS) ? "PENDING" : "PROCESSED";
                String processedAt = status.equals("PROCESSED")
                    ? LocalDate.of(PAYROLL_YEAR, month, 28).format(DATE_FMT) : "";

                rows.add(new String[]{
                    String.format("PE%08d", ++count),
                    empIds.get(ei), deptId,
                    String.valueOf(month), String.valueOf(PAYROLL_YEAR),
                    String.valueOf(base),
                    String.valueOf(otPay),
                    String.valueOf(abseDed),
                    String.valueOf(bonus),
                    String.valueOf(tax),
                    String.valueOf(net),
                    status, "0", processedAt
                });
            }
        }
        writeCsv("payroll_entries.csv", rows);
    }

    // ─── 7. payroll_runs.csv ─────────────────────────────────────────────────
    static void generatePayrollRuns() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "runId","month","year","triggeredBy",
            "totalEmployees","totalNetPay","status","startedAt","completedAt"
        });

        for (int month = 1; month <= NUM_MONTHS; month++) {
            long totalNet = 8_000_000L * NUM_EMPLOYEES + RNG.nextInt(500_000_000);
            rows.add(new String[]{
                String.format("RUN%04d", month),
                String.valueOf(month), String.valueOf(PAYROLL_YEAR),
                "SYSTEM",
                String.valueOf(NUM_EMPLOYEES),
                String.valueOf(totalNet),
                month == NUM_MONTHS ? "PENDING" : "COMPLETED",
                LocalDate.of(PAYROLL_YEAR, month, 25).format(DATE_FMT),
                month == NUM_MONTHS ? "" : LocalDate.of(PAYROLL_YEAR, month, 28).format(DATE_FMT)
            });
        }
        writeCsv("payroll_runs.csv", rows);
    }

    // ─── Utility: ghi CSV ────────────────────────────────────────────────────
    static void writeCsv(String filename, List<String[]> rows) throws IOException {
        Path path = Paths.get(outputDir, filename);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            for (String[] row : rows) {
                bw.write(escapeCsvRow(row));
                bw.newLine();
            }
        }
        // In thống kê (trừ header)
        int dataRows = rows.size() - 1;
        System.out.printf("  ✓ %-30s %,6d dòng%n", filename, dataRows);
    }

    static String escapeCsvRow(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String f = fields[i] == null ? "" : fields[i];
            // Bọc nháy kép nếu có dấu phẩy hoặc nháy
            if (f.contains(",") || f.contains("\"") || f.contains("\n")) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    static String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = normalized.replaceAll("\\p{M}", "");
        return stripped.replace("Đ", "D").replace("đ", "d");
    }
}
