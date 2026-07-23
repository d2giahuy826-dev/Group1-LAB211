package model;

/**
 * AttendanceRecord – Bảng chấm công theo tháng của nhân viên.
 *
 * CSV columns (8):
 *   attendId, empId, month, year,
 *   workDays, absenceDays, overtimeHours, lateCount
 *
 * Lưu ý: CSV thực tế lưu dữ liệu TỔNG HỢP theo tháng (không phải checkIn/checkOut
 * từng ngày), nên model này dùng các trường tổng hợp tương ứng.
 */
public class AttendanceRecord extends BaseEntity {

    private String empId;
    private int month;
    private int year;
    private int workDays;
    private int absenceDays;
    private int overtimeHours;
    private int lateCount;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public AttendanceRecord() {}

    public AttendanceRecord(String attendId, String empId, int month, int year,
                            int workDays, int absenceDays,
                            int overtimeHours, int lateCount) {
        super(attendId);
        setEmpId(empId);
        setMonth(month);
        setYear(year);
        setWorkDays(workDays);
        setAbsenceDays(absenceDays);
        setOvertimeHours(overtimeHours);
        setLateCount(lateCount);
    }

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 8 cột.
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id, empId,
                String.valueOf(month),
                String.valueOf(year),
                String.valueOf(workDays),
                String.valueOf(absenceDays),
                String.valueOf(overtimeHours),
                String.valueOf(lateCount)
        );
    }

    /**
     * Parse từ dòng CSV (8 cột). Chuyen dt vao chuoi csv. Doc tu chuoi csv vao doi tuong
     * Format: attendId,empId,month,year,workDays,absenceDays,overtimeHours,lateCount
     */
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException(
                    "Invalid AttendanceRecord CSV: expected 8 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }

        setId(parts[0].trim());
        setEmpId(parts[1].trim());
        setMonth(Integer.parseInt(parts[2].trim()));
        setYear(Integer.parseInt(parts[3].trim()));
        setWorkDays(Integer.parseInt(parts[4].trim()));
        setAbsenceDays(Integer.parseInt(parts[5].trim()));
        setOvertimeHours(Integer.parseInt(parts[6].trim()));
        setLateCount(Integer.parseInt(parts[7].trim()));
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    /**
     * Tính tổng giờ làm việc trong tháng (không tính OT).
     */
    public double calculateTotalWorkHours() {
        return workDays * 8.0;
    }

    /**
     * Kiểm tra nhân viên có đi làm đủ ngày không (absenceDays == 0).
     */
    public boolean hasFullAttendance() {
        return absenceDays == 0;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) {
        validateRequired(empId, "Employee ID");
        this.empId = empId.trim();
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

    public int getWorkDays() { return workDays; }
    public void setWorkDays(int workDays) {
        validateNonNegative(workDays, "Work days");
        this.workDays = workDays;
    }

    public int getAbsenceDays() { return absenceDays; }
    public void setAbsenceDays(int absenceDays) {
        validateNonNegative(absenceDays, "Absence days");
        this.absenceDays = absenceDays;
    }

    public int getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(int overtimeHours) {
        validateNonNegative(overtimeHours, "Overtime hours");
        this.overtimeHours = overtimeHours;
    }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) {
        validateNonNegative(lateCount, "Late count");
        this.lateCount = lateCount;
    }

    @Override
    public String toString() {
        return "AttendanceRecord{id='" + id + "', empId='" + empId
               + "', " + month + "/" + year
               + ", workDays=" + workDays + ", absenceDays=" + absenceDays
               + ", OT=" + overtimeHours + "h, late=" + lateCount + "}";
    }
}
