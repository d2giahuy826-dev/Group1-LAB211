package model;

/**
 * LeaveBalance – Số dư nghỉ phép của nhân viên trong một năm.
 *
 * CSV columns (10):
 *   balanceId, empId, year,
 *   annualTotal, annualUsed, annualRemaining,
 *   sickTotal, sickUsed, sickRemaining,
 *   version  ← Optimistic Locking để chống sai lệch khi nhiều thread cập nhật
 */
public class LeaveBalance extends BaseEntity {

    private String empId;
    private int year;

    // Annual leave
    private int annualTotal;
    private int annualUsed;
    private int annualRemaining;

    // Sick leave
    private int sickTotal;
    private int sickUsed;
    private int sickRemaining;

    /** Optimistic Lock – tăng lên 1 mỗi lần update thành công */
    private int version;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public LeaveBalance() {}

    public LeaveBalance(String balanceId, String empId, int year,
                        int annualTotal, int annualUsed,
                        int sickTotal, int sickUsed,
                        int version) {
        super(balanceId);
        setEmpId(empId);
        setYear(year);
        setAnnualTotal(annualTotal);
        setAnnualUsed(annualUsed);
        this.annualRemaining = annualTotal - annualUsed;
        setSickTotal(sickTotal);
        setSickUsed(sickUsed);
        this.sickRemaining = sickTotal - sickUsed;
        setVersion(version);
    }

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 10 cột.
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id, empId,
                String.valueOf(year),
                String.valueOf(annualTotal),
                String.valueOf(annualUsed),
                String.valueOf(annualRemaining),
                String.valueOf(sickTotal),
                String.valueOf(sickUsed),
                String.valueOf(sickRemaining),
                String.valueOf(version)
        );
    }

    /**
     * Parse từ dòng CSV (10 cột).
     * Format: balanceId,empId,year,annualTotal,annualUsed,annualRemaining,
     *         sickTotal,sickUsed,sickRemaining,version
     */
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",", -1);
        if (parts.length != 10) {
            throw new IllegalArgumentException(
                    "Invalid LeaveBalance CSV: expected 10 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }

        setId(parts[0].trim());
        setEmpId(parts[1].trim());
        setYear(Integer.parseInt(parts[2].trim()));
        setAnnualTotal(Integer.parseInt(parts[3].trim()));
        setAnnualUsed(Integer.parseInt(parts[4].trim()));
        this.annualRemaining = Integer.parseInt(parts[5].trim());
        setSickTotal(Integer.parseInt(parts[6].trim()));
        setSickUsed(Integer.parseInt(parts[7].trim()));
        this.sickRemaining = Integer.parseInt(parts[8].trim());
        setVersion(Integer.parseInt(parts[9].trim()));
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    /**
     * Trừ ngày nghỉ phép theo loại.
     * Ném IllegalStateException nếu không đủ số dư.
     *
     * @param type loại nghỉ (ANNUAL hoặc SICK)
     * @param days số ngày cần trừ
     */
    public void deductLeave(LeaveType type, int days) {
        validateNonNegative(days, "Days to deduct");
        if (type == LeaveType.ANNUAL) {
            if (annualRemaining < days)
                throw new IllegalStateException(
                        "Insufficient annual leave: remaining=" + annualRemaining + ", requested=" + days);
            annualUsed += days;
            annualRemaining -= days;
        } else {
            if (sickRemaining < days)
                throw new IllegalStateException(
                        "Insufficient sick leave: remaining=" + sickRemaining + ", requested=" + days);
            sickUsed += days;
            sickRemaining -= days;
        }
    }

    /**
     * Kiểm tra còn đủ ngày nghỉ không.
     */
    public boolean hasEnoughLeave(LeaveType type, int days) {
        return type == LeaveType.ANNUAL
                ? annualRemaining >= days
                : sickRemaining >= days;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) {
        validateRequired(empId, "Employee ID");
        this.empId = empId.trim();
    }

    public int getYear() { return year; }
    public void setYear(int year) {
        if (year < 2000 || year > 2100)
            throw new IllegalArgumentException("Year out of range: " + year);
        this.year = year;
    }

    public int getAnnualTotal() { return annualTotal; }
    public void setAnnualTotal(int annualTotal) {
        validateNonNegative(annualTotal, "Annual total");
        this.annualTotal = annualTotal;
    }

    public int getAnnualUsed() { return annualUsed; }
    public void setAnnualUsed(int annualUsed) {
        validateNonNegative(annualUsed, "Annual used");
        this.annualUsed = annualUsed;
        this.annualRemaining = this.annualTotal - annualUsed;
    }

    public int getAnnualRemaining() { return annualRemaining; }

    public int getSickTotal() { return sickTotal; }
    public void setSickTotal(int sickTotal) {
        validateNonNegative(sickTotal, "Sick total");
        this.sickTotal = sickTotal;
    }

    public int getSickUsed() { return sickUsed; }
    public void setSickUsed(int sickUsed) {
        validateNonNegative(sickUsed, "Sick used");
        this.sickUsed = sickUsed;
        this.sickRemaining = this.sickTotal - sickUsed;
    }

    public int getSickRemaining() { return sickRemaining; }

    public int getVersion() { return version; }
    public void setVersion(int version) {
        validateNonNegative(version, "Version");
        this.version = version;
    }

    @Override
    public String toString() {
        return "LeaveBalance{id='" + id + "', empId='" + empId + "', year=" + year
               + ", annual=" + annualUsed + "/" + annualTotal
               + ", sick=" + sickUsed + "/" + sickTotal
               + ", version=" + version + "}";
    }
}
