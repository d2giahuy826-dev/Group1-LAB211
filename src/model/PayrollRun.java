package model;

/**
 * PayrollRun – Đợt chạy lương toàn công ty (1 lần/tháng).
 *
 * CSV columns (9):
 *   runId, month, year, triggeredBy,
 *   totalEmployees, totalNetPay, status, startedAt, completedAt
 *
 * QUAN TRỌNG:
 *  - status dùng RunStatus enum (PENDING / COMPLETED)
 *  - completedAt = "" nếu chưa hoàn thành
 */
public class PayrollRun extends BaseEntity {

    private int month;
    private int year;
    private String triggeredBy;
    private int totalEmployees;
    private long totalNetPay;

    /** Trạng thái đợt chạy lương – dùng RunStatus (không phải PayrollStatus) */
    private RunStatus status;

    private String startedAt;
    private String completedAt;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public PayrollRun() {}

    public PayrollRun(String runId, int month, int year, String triggeredBy,
                      int totalEmployees, long totalNetPay, RunStatus status,
                      String startedAt, String completedAt) {
        super(runId);
        setMonth(month);
        setYear(year);
        setTriggeredBy(triggeredBy);
        setTotalEmployees(totalEmployees);
        setTotalNetPay(totalNetPay);
        setStatus(status);
        setStartedAt(startedAt);
        setCompletedAt(completedAt);
    }

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 9 cột.
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id,
                String.valueOf(month),
                String.valueOf(year),
                triggeredBy,
                String.valueOf(totalEmployees),
                String.valueOf(totalNetPay),
                status.name(),
                startedAt == null ? "" : startedAt,
                completedAt == null ? "" : completedAt
        );
    }

    /**
     * Parse từ dòng CSV (9 cột).
     * Format: runId,month,year,triggeredBy,totalEmployees,totalNetPay,
     *         status,startedAt,completedAt
     */
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException(
                    "Invalid PayrollRun CSV: expected 9 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }

        setId(parts[0].trim());
        setMonth(Integer.parseInt(parts[1].trim()));
        setYear(Integer.parseInt(parts[2].trim()));
        setTriggeredBy(parts[3].trim());
        setTotalEmployees(Integer.parseInt(parts[4].trim()));
        setTotalNetPay(Long.parseLong(parts[5].trim()));
        setStatus(RunStatus.fromString(parts[6].trim()));
        setStartedAt(parts[7].trim());
        setCompletedAt(parts[8].trim());
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    /**
     * Kiểm tra đợt chạy lương đã hoàn thành chưa.
     */
    public boolean isCompleted() {
        return status != null && status.isCompleted();
    }

    /**
     * Đánh dấu hoàn thành đợt chạy lương.
     */
    public void markCompleted(String completedDate) {
        this.status = RunStatus.COMPLETED;
        this.completedAt = completedDate;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int getMonth() { return month; }
    public void setMonth(int month) {
        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Month must be 1-12. Got: " + month);
        this.month = month;
    }

    public int getYear() { return year; }
    public void setYear(int year) {
        if (year < 2000 || year > 2100)
            throw new IllegalArgumentException("Year out of range: " + year);
        this.year = year;
    }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) {
        validateRequired(triggeredBy, "Triggered by");
        this.triggeredBy = triggeredBy.trim();
    }

    public int getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(int totalEmployees) {
        validateNonNegative(totalEmployees, "Total employees");
        this.totalEmployees = totalEmployees;
    }

    public long getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(long totalNetPay) {
        validateNonNegative(totalNetPay, "Total net pay");
        this.totalNetPay = totalNetPay;
    }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        this.status = status;
    }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt == null ? "" : startedAt.trim();
    }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt == null ? "" : completedAt.trim();
    }

    @Override
    public String toString() {
        return "PayrollRun{id='" + id + "', " + month + "/" + year
               + ", status=" + status + ", employees=" + totalEmployees
               + ", totalNetPay=" + totalNetPay + "}";
    }
}
