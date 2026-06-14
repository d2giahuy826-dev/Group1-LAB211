package model;

/**
 * SalaryCalculator – Model class chứa toàn bộ logic tính lương.
 *
 * Theo rubric LAB211: logic tính lương phải nằm trong Model,
 * KHÔNG được đặt ở Controller/View.
 *
 * Công thức:
 *   overtimePay       = baseSalary / STANDARD_WORK_DAYS / HOURS_PER_DAY
 *                       × overtimeHours × OT_MULTIPLIER (1.5)
 *   absenceDeduction  = baseSalary / STANDARD_WORK_DAYS × absenceDays
 *   attendanceBonus   = baseSalary × BONUS_RATE (5%) nếu absenceDays == 0
 *   grossSalary       = baseSalary + overtimePay − absenceDeduction + bonus
 *   taxAmount         = grossSalary × taxRate
 *   netSalary         = grossSalary − taxAmount
 */
public class SalaryCalculator {

    // ─── Hằng số lương ────────────────────────────────────────────────
    /** Số ngày công chuẩn mỗi tháng */
    public final int STANDARD_WORK_DAYS = 22;

    /** Số giờ làm việc mỗi ngày */
    public final int HOURS_PER_DAY = 8;

    /** Hệ số lương làm thêm giờ (overtime) */
    public final double OT_MULTIPLIER = 1.5;

    /** Tỷ lệ thưởng đi đủ ngày (attendance bonus) */
    public final double BONUS_RATE = 0.05;

    // ─── Input fields ─────────────────────────────────────────────────
    private double baseSalary;
    private int overtimeHours;
    private int absenceDays;
    private double taxRate;

    // ─── Computed results (cache sau khi calculate()) ─────────────────
    private double overtimePay;
    private double absenceDeduction;
    private double attendanceBonus;
    private double grossSalary;
    private double taxAmount;
    private double netSalary;
    private boolean calculated = false;

    // ─── Constructors ─────────────────────────────────────────────────

    public SalaryCalculator() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param baseSalary    lương cơ bản (≥ 0)
     * @param overtimeHours số giờ OT (≥ 0)
     * @param absenceDays   số ngày nghỉ không phép (≥ 0)
     * @param taxRate       thuế suất (0 – 1)
     */
    public SalaryCalculator(double baseSalary, int overtimeHours,
                            int absenceDays, double taxRate) {
        setBaseSalary(baseSalary);
        setOvertimeHours(overtimeHours);
        setAbsenceDays(absenceDays);
        setTaxRate(taxRate);
    }

    // ─── Factory methods tiện dụng ────────────────────────────────────

    /**
     * Tạo calculator cho nhân viên FULLTIME (taxRate = 10%).
     */
    

    /**
     * Tạo calculator cho nhân viên PARTTIME (taxRate = 5%).
     */
   

    // ─── Core calculation ─────────────────────────────────────────────

    /**
     * Thực hiện tính toán toàn bộ các thành phần lương.
     * Gọi method này trước khi truy cập kết quả.
     *
     * @return this (fluent API)
     */
    public SalaryCalculator calculate() {
        // 1. Overtime pay = (baseSalary / 22 / 8) × otHours × 1.5
        double dailyRate = baseSalary / STANDARD_WORK_DAYS;
        double hourlyRate = dailyRate / HOURS_PER_DAY;
        this.overtimePay = Math.round(hourlyRate * overtimeHours * OT_MULTIPLIER);

        // 2. Absence deduction = (baseSalary / 22) × absenceDays
        this.absenceDeduction = Math.round(dailyRate * absenceDays);

        // 3. Attendance bonus = baseSalary × 5% nếu đi đủ ngày (absence == 0)
        this.attendanceBonus = (absenceDays == 0)
                ? Math.round(baseSalary * BONUS_RATE)
                : 0;

        // 4. Gross = base + OT − absence + bonus
        this.grossSalary = baseSalary + overtimePay - absenceDeduction + attendanceBonus;

        // 5. Tax = gross × taxRate
        this.taxAmount = Math.round(grossSalary * taxRate);

        // 6. Net = gross − tax
        this.netSalary = grossSalary - taxAmount;

        this.calculated = true;
        return this;
    }

    // ─── Getters (kết quả) ────────────────────────────────────────────

    /**
     * Tiền làm thêm giờ: baseSalary / 22 / 8 × otHours × 1.5
     */
    public double getOvertimePay() {
        ensureCalculated();
        return overtimePay;
    }

    /**
     * Tiền trừ nghỉ: baseSalary / 22 × absenceDays
     */
    public double getAbsenceDeduction() {
        ensureCalculated();
        return absenceDeduction;
    }

    /**
     * Thưởng đi đủ ngày: baseSalary × 5% (chỉ khi absenceDays == 0)
     */
    public double getAttendanceBonus() {
        ensureCalculated();
        return attendanceBonus;
    }

    /**
     * Tổng lương trước thuế (gross).
     */
    public double getGrossSalary() {
        ensureCalculated();
        return grossSalary;
    }

    /**
     * Tiền thuế: gross × taxRate
     */
    public double getTaxAmount() {
        ensureCalculated();
        return taxAmount;
    }

    /**
     * Lương thực nhận (net): gross − tax
     */
    public double getNetSalary() {
        ensureCalculated();
        return netSalary;
    }

    // ─── Getters / Setters (input) ────────────────────────────────────

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        if (baseSalary < 0) {
            throw new IllegalArgumentException("Base salary must not be negative.");
        }
        this.baseSalary = baseSalary;
        this.calculated = false;
    }

    public int getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(int overtimeHours) {
        if (overtimeHours < 0) {
            throw new IllegalArgumentException("Overtime hours must not be negative.");
        }
        this.overtimeHours = overtimeHours;
        this.calculated = false;
    }

    public int getAbsenceDays() {
        return absenceDays;
    }

    public void setAbsenceDays(int absenceDays) {
        if (absenceDays < 0) {
            throw new IllegalArgumentException("Absence days must not be negative.");
        }
        this.absenceDays = absenceDays;
        this.calculated = false;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        if (taxRate < 0 || taxRate > 1) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 1.");
        }
        this.taxRate = taxRate;
        this.calculated = false;
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private void ensureCalculated() {
        if (!calculated) {
            throw new IllegalStateException(
                    "Salary not calculated yet. Call calculate() first.");
        }
    }

    @Override
    public String toString() {
        if (!calculated) {
            return "SalaryCalculator{not calculated yet}";
        }
        return String.format(
            "SalaryCalculator{base=%.0f, otPay=%.0f, absence=%.0f, " +
            "bonus=%.0f, gross=%.0f, tax=%.0f, net=%.0f}",
            baseSalary, overtimePay, absenceDeduction,
            attendanceBonus, grossSalary, taxAmount, netSalary);
    }
}
