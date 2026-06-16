package test;

import model.SalaryCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 – Unit tests cho SalaryCalculator
 * LAB211 – Employee Payroll Management Simulation
 */
@DisplayName("SalaryCalculator Tests")
class SalaryCalculatorTest {

    // ─── Hằng số dùng chung ──────────────────────────────────────────────────
    private static final int    WORKING_DAYS   = 22;
    private static final int    HOURS_PER_DAY  = 8;
    private static final double DELTA          = 1.0; // sai số làm tròn cho double

    // ════════════════════════════════════════════════════════════════════════
    // TC01 – FULLTIME | Đủ ngày | OT 10 giờ | Base 12,000,000
    // ════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC01 – Fulltime, đủ ngày, OT 10 giờ → tính đúng OT, bonus, net > base")
    void testFulltime_DuNgay_CoOT() {
        double base = 12_000_000.0;
        int    otHours = 10;

        SalaryCalculator calc = SalaryCalculator.forFulltime(base, otHours, 0);
        calc.calculate();

        // hourlyRate = 12,000,000 / 22 / 8 ≈ 68,181.818…
        double hourlyRate   = base / WORKING_DAYS / HOURS_PER_DAY;
        double expectedOT   = Math.round(hourlyRate * otHours * 1.5);
        double expectedBonus = Math.round(base * 0.05);
        double expectedGross = base + expectedOT + expectedBonus;
        double expectedTax  = Math.round(expectedGross * 0.10);
        double expectedNet  = expectedGross - expectedTax;

        assertAll("TC01 assertions",
            () -> assertEquals(expectedOT,    calc.getOvertimePay(),    DELTA, "OT pay sai"),
            () -> assertEquals(expectedBonus, calc.getAttendanceBonus(),DELTA, "Attendance bonus sai"),
            () -> assertEquals(expectedGross, calc.getGrossSalary(),    DELTA, "Gross salary sai"),
            () -> assertEquals(expectedNet,   calc.getNetSalary(),      DELTA, "Net salary sai"),
            () -> assertTrue(calc.getNetSalary() > base, "Net phải lớn hơn base khi có OT và bonus")
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC02 – PARTTIME | Đủ ngày | Không OT | Base 8,000,000
    // ════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC02 – Parttime, đủ ngày, không OT → thuế 5%, net đúng")
    void testParttime_DuNgay_KhongOT() {
        double base = 8_000_000.0;

        SalaryCalculator calc = SalaryCalculator.forParttime(base, 0, 0);
        calc.calculate();

        double expectedBonus = Math.round(base * 0.05);
        double expectedGross = base + expectedBonus;
        double expectedTax   = Math.round(expectedGross * 0.05); // Parttime: 5%
        double expectedNet   = expectedGross - expectedTax;

        assertAll("TC02 assertions",
            () -> assertEquals(expectedBonus, calc.getAttendanceBonus(), DELTA, "Attendance bonus sai"),
            () -> assertEquals(expectedTax,   calc.getTaxAmount(),        DELTA, "Tax (5%) sai"),
            () -> assertEquals(expectedGross, calc.getGrossSalary(),      DELTA, "Gross salary sai"),
            () -> assertEquals(expectedNet,   calc.getNetSalary(),        DELTA, "Net salary sai")
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC03 – FULLTIME | Nghỉ không phép 2 ngày | Mất bonus
    // ════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC03 – Fulltime, nghỉ 2 ngày không phép → mất bonus, bị khấu trừ, net < base")
    void testNghiKhongPhep_MatBonus() {
        double base          = 12_000_000.0;
        int    absenceDays   = 2;

        SalaryCalculator calc = SalaryCalculator.forFulltime(base, 0, absenceDays);
        calc.calculate();

        double expectedDeduction = Math.round(base / WORKING_DAYS * absenceDays);

        assertAll("TC03 assertions",
            () -> assertEquals(0.0,               calc.getAttendanceBonus(),   DELTA, "Bonus phải bằng 0 khi nghỉ"),
            () -> assertEquals(expectedDeduction,  calc.getAbsenceDeduction(),  DELTA, "Absence deduction sai"),
            () -> assertTrue(calc.getNetSalary() < base, "Net phải nhỏ hơn base khi nghỉ không phép")
        );
    }

}