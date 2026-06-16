package model;

public class SalaryCalculator {

    // ─── Hằng số lương ────────────────────────────────────────────────
    public final int    STANDARD_WORK_DAYS        = 22;
    public final int    HOURS_PER_DAY             = 8;
    public final double OT_MULTIPLIER             = 1.5;
    public final double ATTENDANCE_BONUS_RATE     = 0.05;
    public final double DEFAULT_FULLTIME_TAX_RATE = 0.10;
    public final double DEFAULT_PARTTIME_TAX_RATE = 0.05;

    // ─── Input fields ─────────────────────────────────────────────────
    private double baseSalary;
    private int    overtimeHours;
    private int    absenceDays;
    private double taxRate;

    // ─── Computed results ─────────────────────────────────────────────
    private double  overtimePay;
    private double  absenceDeduction;
    private double  attendanceBonus;
    private double  grossSalary;
    private double  taxAmount;
    private double  netSalary;
    private boolean calculated = false;

    // ─── Constructors ─────────────────────────────────────────────────

    public SalaryCalculator() {
    }

    public SalaryCalculator(double baseSalary, int overtimeHours,
                            int absenceDays, double taxRate) {
        setBaseSalary(baseSalary);
        setOvertimeHours(overtimeHours);
        setAbsenceDays(absenceDays);
        setTaxRate(taxRate);
    }

    public SalaryCalculator(double baseSalary, int overtimeHours,
                            int absenceDays, EmpType type) {
        setBaseSalary(baseSalary);
        setOvertimeHours(overtimeHours);
        setAbsenceDays(absenceDays);
        setTaxRate(type == EmpType.FULLTIME
                ? DEFAULT_FULLTIME_TAX_RATE
                : DEFAULT_PARTTIME_TAX_RATE);
    }

    // ─── Factory methods ──────────────────────────────────────────────

    public static SalaryCalculator forFulltime(double baseSalary,
                                               int overtimeHours,
                                               int absenceDays) {
        return new SalaryCalculator(baseSalary, overtimeHours,
                                    absenceDays, 0.10);
    }

    public static SalaryCalculator forParttime(double baseSalary,
                                               int overtimeHours,
                                               int absenceDays) {
        return new SalaryCalculator(baseSalary, overtimeHours,
                                    absenceDays, 0.05);
    }

    // ─── Core calculation ─────────────────────────────────────────────

    public SalaryCalculator calculate() {
        // 1. Overtime pay
        double dailyRate  = baseSalary / STANDARD_WORK_DAYS;
        double hourlyRate = dailyRate / HOURS_PER_DAY;
        overtimePay = Math.round(hourlyRate * overtimeHours * OT_MULTIPLIER);

        // 2. Absence deduction
        absenceDeduction = Math.round(dailyRate * absenceDays);

        // 3. Attendance bonus = 5% base nếu đi đủ ngày
        attendanceBonus = (absenceDays == 0)
                ? Math.round(baseSalary * ATTENDANCE_BONUS_RATE)
                : 0;

        // 4. Gross = base + OT − absence + bonus
        grossSalary = baseSalary + overtimePay - absenceDeduction + attendanceBonus;

        // 5. Tax
        taxAmount = Math.round(grossSalary * taxRate);

        // 6. Net
        netSalary = grossSalary - taxAmount;

        calculated = true;
        return this;
    }

    // ─── Getters (kết quả) ────────────────────────────────────────────

    public double getOvertimePay() {
        ensureCalculated();
        return overtimePay;
    }

    public double getAbsenceDeduction() {
        ensureCalculated();
        return absenceDeduction;
    }

    public double getAttendanceBonus() {
        ensureCalculated();
        return attendanceBonus;
    }

    public double getGrossSalary() {
        ensureCalculated();
        return grossSalary;
    }

    public double getTaxAmount() {
        ensureCalculated();
        return taxAmount;
    }

    public double getNetSalary() {
        ensureCalculated();
        return netSalary;
    }

    // ─── Getters / Setters (input) ────────────────────────────────────

    public double getBaseSalary() { return baseSalary; }

    public void setBaseSalary(double baseSalary) {
        if (baseSalary < 0)
            throw new IllegalArgumentException("Base salary must not be negative.");
        this.baseSalary = baseSalary;
        calculated = false;
    }

    public int getOvertimeHours() { return overtimeHours; }

    public void setOvertimeHours(int overtimeHours) {
        if (overtimeHours < 0)
            throw new IllegalArgumentException("Overtime hours must not be negative.");
        this.overtimeHours = overtimeHours;
        calculated = false;
    }

    public int getAbsenceDays() { return absenceDays; }

    public void setAbsenceDays(int absenceDays) {
        if (absenceDays < 0)
            throw new IllegalArgumentException("Absence days must not be negative.");
        this.absenceDays = absenceDays;
        calculated = false;
    }

    public double getTaxRate() { return taxRate; }

    public void setTaxRate(double taxRate) {
        if (taxRate < 0 || taxRate > 1)
            throw new IllegalArgumentException("Tax rate must be between 0 and 1.");
        this.taxRate = taxRate;
        calculated = false;
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