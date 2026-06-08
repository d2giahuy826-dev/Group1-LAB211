package model;

import java.time.LocalDate;

/**
 * PayrollEntry – Dòng lương cá nhân trong một tháng.
 *
 * CSV columns (14):
 *   entryId, empId, deptId, month, year,
 *   baseSalary, overtimePay, absenceDeduction, bonus, taxAmount, netSalary,
 *   status, version, processedAt
 *
 * QUAN TRỌNG:
 *  - version dùng cho Optimistic Locking → chống Double Payment
 *  - status dùng PayStatus enum (PENDING / PROCESSED)
 */
public class PayrollEntry extends BaseEntity {

    private String empId;
    private String deptId;
    private int month;
    private int year;

    // Các thành phần lương
    private long baseSalary;
    private long overtimePay;
    private long absenceDeduction;
    private long bonus;
    private long taxAmount;
    private long netSalary;

    /** Trạng thái xử lý lương – dùng PayStatus (không phải PayrollStatus) */
    private PayStatus status;

    /** Optimistic Lock – chống Double Payment khi nhiều thread xử lý đồng thời */
    private int version;

    private String processedAt; // "" nếu chưa xử lý

    // ─── Constructors ─────────────────────────────────────────────────────────

    public PayrollEntry() {}

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 14 cột.
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id, empId, deptId,
                String.valueOf(month),
                String.valueOf(year),
                String.valueOf(baseSalary),
                String.valueOf(overtimePay),
                String.valueOf(absenceDeduction),
                String.valueOf(bonus),
                String.valueOf(taxAmount),
                String.valueOf(netSalary),
                status.name(),
                String.valueOf(version),
                processedAt == null ? "" : processedAt
        );
    }

    /**
     * Parse từ dòng CSV (14 cột).
     * Format: entryId,empId,deptId,month,year,baseSalary,overtimePay,
     *         absenceDeduction,bonus,taxAmount,netSalary,status,version,processedAt
     */
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",", -1);
        if (parts.length != 14) {
            throw new IllegalArgumentException(
                    "Invalid PayrollEntry CSV: expected 14 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }

        setId(parts[0].trim());
        setEmpId(parts[1].trim());
        setDeptId(parts[2].trim());
        setMonth(Integer.parseInt(parts[3].trim()));
        setYear(Integer.parseInt(parts[4].trim()));
        setBaseSalary(Long.parseLong(parts[5].trim()));
        setOvertimePay(Long.parseLong(parts[6].trim()));
        setAbsenceDeduction(Long.parseLong(parts[7].trim()));
        setBonus(Long.parseLong(parts[8].trim()));
        setTaxAmount(Long.parseLong(parts[9].trim()));
        setNetSalary(Long.parseLong(parts[10].trim()));
        setStatus(PayStatus.fromString(parts[11].trim()));
        setVersion(Integer.parseInt(parts[12].trim()));
        this.processedAt = parts[13].trim();
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    /**
     * Kiểm tra entry đã được xử lý chưa.
     */
    public boolean isProcessed() {
        return status != null && status.isProcessed();
    }

    /**
     * Đánh dấu entry đã xử lý và tăng version (Optimistic Lock).
     * Gọi sau khi tính lương thành công.
     */
    public void markProcessed(String processedDate) {
        this.status = PayStatus.PROCESSED;
        this.processedAt = processedDate;
        this.version++;  // Tăng version → các thread khác sẽ thấy conflict
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) {
        validateRequired(empId, "Employee ID");
        this.empId = empId.trim();
    }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) {
        validateRequired(deptId, "Department ID");
        this.deptId = deptId.trim();
    }

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

    public long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(long baseSalary) {
        validateNonNegative(baseSalary, "Base salary");
        this.baseSalary = baseSalary;
    }

    public long getOvertimePay() { return overtimePay; }
    public void setOvertimePay(long overtimePay) {
        validateNonNegative(overtimePay, "Overtime pay");
        this.overtimePay = overtimePay;
    }

    public long getAbsenceDeduction() { return absenceDeduction; }
    public void setAbsenceDeduction(long absenceDeduction) {
        validateNonNegative(absenceDeduction, "Absence deduction");
        this.absenceDeduction = absenceDeduction;
    }

    public long getBonus() { return bonus; }
    public void setBonus(long bonus) {
        validateNonNegative(bonus, "Bonus");
        this.bonus = bonus;
    }

    public long getTaxAmount() { return taxAmount; }
    public void setTaxAmount(long taxAmount) {
        validateNonNegative(taxAmount, "Tax amount");
        this.taxAmount = taxAmount;
    }

    public long getNetSalary() { return netSalary; }
    public void setNetSalary(long netSalary) {
        this.netSalary = netSalary; // có thể âm nếu deduction > base
    }

    public PayStatus getStatus() { return status; }
    public void setStatus(PayStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        this.status = status;
    }

    public int getVersion() { return version; }
    public void setVersion(int version) {
        validateNonNegative(version, "Version");
        this.version = version;
    }

    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt == null ? "" : processedAt.trim();
    }

    @Override
    public String toString() {
        return "PayrollEntry{id='" + id + "', empId='" + empId
               + "', " + month + "/" + year
               + ", net=" + netSalary + ", status=" + status
               + ", version=" + version + "}";
    }
}
