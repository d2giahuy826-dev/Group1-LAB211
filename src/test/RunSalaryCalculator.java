package test;


import model.SalaryCalculator;

public class RunSalaryCalculator {

    public static void main(String[] args) {

        RunSalaryCalculator test = new RunSalaryCalculator();

        System.out.println("==============================================");
        System.out.println("     SALARY CALCULATOR TEST  LAB211");
        System.out.println("==============================================");

        test.testFulltime_DuNgay_CoOT();
        test.testParttime_DuNgay_KhongOT();
        test.testNghiKhongPhep_MatBonus();
       

        System.out.println("==============================================");
    }

    // ─── TC01: FULLTIME, di du ngay, co OT ───────────────────────────────────
    void testFulltime_DuNgay_CoOT() {

        System.out.println("\n[TC01] FULLTIME | Du ngay | OT 10 gio | Base 12,000,000");

        // Fix: baseSalary phai la double (12_000_000.0)
        SalaryCalculator calc = SalaryCalculator.forFulltime(12_000_000.0, 10, 0);
        calc.calculate();

        // hourlyRate = 12,000,000 / 22 / 8 = 68,181.8
        // overtimePay = round(68,181.8 * 10 * 1.5) = 1,022,727
        // attendanceBonus = 500,000 (cố định vì đi đủ ngày)
        // gross = 12,000,000 + 1,022,727 + 500,000 = 13,522,727
        // tax = round(13,522,727 * 0.10) = 1,352,273
        // net = 13,522,727 - 1,352,273 = 12,170,454

        System.out.println("  OT pay            : " + (long) calc.getOvertimePay());
        System.out.println("  Attendance bonus  : " + (long) calc.getAttendanceBonus());
        System.out.println("  Gross salary      : " + (long) calc.getGrossSalary());
        System.out.println("  Net salary        : " + (long) calc.getNetSalary());

        if (calc.getOvertimePay() > 0 && calc.getAttendanceBonus() == 500_000 && calc.getNetSalary() > 12_000_000) {
            System.out.println("  PASS");
        } else {
            System.out.println("  FAIL");
        }
    }

    // ─── TC02: PARTTIME, di du ngay, khong OT ────────────────────────────────
    void testParttime_DuNgay_KhongOT() {

        System.out.println("\n[TC02] PARTTIME | Du ngay | Khong OT | Base 8,000,000");

        // Fix: them forParttime, baseSalary la double
        SalaryCalculator calc = SalaryCalculator.forParttime(8_000_000.0, 0, 0);
        calc.calculate();

        // attendanceBonus = 500,000 (cố định vì đi đủ ngày)
        // gross = 8,000,000 + 500,000 = 8,500,000
        // tax = round(8,500,000 * 0.05) = 425,000  <- PARTTIME thue 5%
        // net = 8,500,000 - 425,000 = 8,075,000

        System.out.println("  Attendance bonus  : " + (long) calc.getAttendanceBonus());
        System.out.println("  Tax (5%)          : " + (long) calc.getTaxAmount());
        System.out.println("  Net salary        : " + (long) calc.getNetSalary());

        double expectedTax = Math.round(calc.getGrossSalary() * 0.05);

        if (Math.abs(calc.getTaxAmount() - expectedTax) < 1) {
            System.out.println("  PASS");
        } else {
            System.out.println("  FAIL – Tax sai, expected: " + (long) expectedTax
                    + " but got: " + (long) calc.getTaxAmount());
        }
    }

    // ─── TC03: Nghi khong phep 2 ngay, mat bonus ─────────────────────────────
    void testNghiKhongPhep_MatBonus() {

        System.out.println("\n[TC03] FULLTIME | Nghi khong phep 2 ngay | Kiem tra mat bonus");

        // Fix: baseSalary la double
        SalaryCalculator calc = SalaryCalculator.forFulltime(12_000_000.0, 0, 2);
        calc.calculate();

        // absenceDeduction = round(12,000,000 / 22 * 2) = 1,090,909
        // attendanceBonus = 0 (vi nghi > 0 ngay, mat bonus 500k)
        // gross = 12,000,000 - 1,090,909 = 10,909,091
        // tax = round(10,909,091 * 0.10) = 1,090,909
        // net = 10,909,091 - 1,090,909 = 9,818,182

        System.out.println("  Absence deduction : " + (long) calc.getAbsenceDeduction());
        System.out.println("  Attendance bonus  : " + (long) calc.getAttendanceBonus());
        System.out.println("  Net salary        : " + (long) calc.getNetSalary());

        if (calc.getAttendanceBonus() == 0 && calc.getAbsenceDeduction() > 0 && calc.getNetSalary() < 12_000_000) {
            System.out.println("  PASS");
        } else {
            System.out.println("  FAIL");
        }
    }

    
    
}