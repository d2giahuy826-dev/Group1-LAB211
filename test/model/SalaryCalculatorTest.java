package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho SalaryCalculator.
 *
 * Các test case bắt buộc:
 *   1. FULLTIME có OT
 *   2. PARTTIME absent
 *   3. bonus attendance (đi đủ ngày)
 *   4. tax deduction
 *   5. no overtime
 *
 * Bổ sung:
 *   - Edge cases (zero salary, validation)
 *   - Factory methods
 *   - calculate() chưa gọi → exception
 */
class SalaryCalculatorTest {

    // ─── Constants dùng trong test ────────────────────────────────────
    private static final double BASE_10M = 10_000_000;  // 10 triệu VND
    private static final double BASE_5M  =  5_000_000;  //  5 triệu VND
    private static final double DELTA    = 1.0;         // sai số làm tròn

    // ═══════════════════════════════════════════════════════════════════
    // TEST 1: FULLTIME có OT (overtime)
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Test 1 – FULLTIME có OT")
    class FulltimeWithOvertimeTest {

        @Test
        @DisplayName("FULLTIME base=10M, OT=10h, absence=0, tax=10% → tính đúng OT 1.5x")
        void fulltimeWithOvertimeAndPerfectAttendance() {
            SalaryCalculator calc = SalaryCalculator
                    .forFulltime(BASE_10M, 10, 0)
                    .calculate();

            // OT = 10_000_000 / 22 / 8 × 10 × 1.5 = 85,227 (rounded)
            double expectedOT = Math.round(BASE_10M / 22.0 / 8 * 10 * 1.5);
            assertEquals(expectedOT, calc.getOvertimePay(), DELTA,
                    "Overtime pay phải tính = base/22/8 × hours × 1.5");

            // Bonus vì absence=0
            double expectedBonus = Math.round(BASE_10M * 0.05);
            assertEquals(expectedBonus, calc.getAttendanceBonus(), DELTA,
                    "Đi đủ ngày phải có bonus 5%");

            // Absence = 0
            assertEquals(0, calc.getAbsenceDeduction(), DELTA);

            // Gross
            double expectedGross = BASE_10M + expectedOT + expectedBonus;
            assertEquals(expectedGross, calc.getGrossSalary(), DELTA);

            // Tax = gross × 10%
            double expectedTax = Math.round(expectedGross * 0.10);
            assertEquals(expectedTax, calc.getTaxAmount(), DELTA);

            // Net
            assertEquals(expectedGross - expectedTax, calc.getNetSalary(), DELTA);
        }

        @Test
        @DisplayName("FULLTIME base=15M, OT=20h, absence=2 → OT + absence, no bonus")
        void fulltimeWithOvertimeAndAbsence() {
            double base = 15_000_000;
            SalaryCalculator calc = SalaryCalculator
                    .forFulltime(base, 20, 2)
                    .calculate();

            double expectedOT = Math.round(base / 22.0 / 8 * 20 * 1.5);
            assertTrue(calc.getOvertimePay() > 0, "OT pay must be positive");
            assertEquals(expectedOT, calc.getOvertimePay(), DELTA);

            // Absence deduction
            double expectedAbsence = Math.round(base / 22.0 * 2);
            assertEquals(expectedAbsence, calc.getAbsenceDeduction(), DELTA);

            // No bonus because absent
            assertEquals(0, calc.getAttendanceBonus(), DELTA,
                    "Có ngày nghỉ → không có bonus");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 2: PARTTIME absent
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Test 2 – PARTTIME absent")
    class ParttimeAbsentTest {

        @Test
        @DisplayName("PARTTIME base=5M, OT=0, absence=3, tax=5% → trừ lương đúng")
        void parttimeWithAbsence() {
            SalaryCalculator calc = SalaryCalculator
                    .forParttime(BASE_5M, 0, 3)
                    .calculate();

            // Absence = 5_000_000 / 22 × 3 = 681,818 (rounded)
            double expectedAbsence = Math.round(BASE_5M / 22.0 * 3);
            assertEquals(expectedAbsence, calc.getAbsenceDeduction(), DELTA,
                    "Trừ lương = base/22 × absenceDays");

            // No OT
            assertEquals(0, calc.getOvertimePay(), DELTA);

            // No bonus (có absent)
            assertEquals(0, calc.getAttendanceBonus(), DELTA);

            // Tax rate = 5% cho PARTTIME
            double expectedGross = BASE_5M - expectedAbsence;
            double expectedTax = Math.round(expectedGross * 0.05);
            assertEquals(expectedTax, calc.getTaxAmount(), DELTA);

            assertEquals(expectedGross - expectedTax, calc.getNetSalary(), DELTA);
        }

        @Test
        @DisplayName("PARTTIME absent nhiều ngày → net salary giảm rõ rệt")
        void parttimeHighAbsence() {
            SalaryCalculator calc = SalaryCalculator
                    .forParttime(BASE_5M, 0, 10)
                    .calculate();

            // 10 ngày absent trên 22 → trừ gần nửa lương
            double deduction = calc.getAbsenceDeduction();
            assertTrue(deduction > BASE_5M * 0.4,
                    "10 ngày nghỉ phải trừ > 40% lương");
            assertTrue(calc.getNetSalary() < BASE_5M * 0.6,
                    "Net sau khi trừ 10 ngày phải < 60% base");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 3: Bonus attendance (đi đủ ngày)
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Test 3 – Bonus attendance")
    class BonusAttendanceTest {

        @Test
        @DisplayName("absence=0 → bonus = base × 5%")
        void perfectAttendanceGivesBonus() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 0, 0.10)
                    .calculate();

            double expectedBonus = Math.round(BASE_10M * 0.05);
            assertEquals(expectedBonus, calc.getAttendanceBonus(), DELTA,
                    "Đi đủ ngày → bonus 5% base salary");
        }

        @Test
        @DisplayName("absence=1 → bonus = 0")
        void oneAbsenceNoBonus() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 1, 0.10)
                    .calculate();

            assertEquals(0, calc.getAttendanceBonus(), DELTA,
                    "Nghỉ 1 ngày → mất bonus");
        }

        @Test
        @DisplayName("So sánh net salary: có bonus vs không bonus")
        void bonusIncreasesNetSalary() {
            SalaryCalculator withBonus = new SalaryCalculator(BASE_10M, 0, 0, 0.10)
                    .calculate();
            SalaryCalculator noBonus = new SalaryCalculator(BASE_10M, 0, 1, 0.10)
                    .calculate();

            assertTrue(withBonus.getNetSalary() > noBonus.getNetSalary(),
                    "Nhân viên đi đủ ngày phải nhận nhiều hơn");

            // Bonus + không bị trừ absent → chênh lệch lớn
            double diff = withBonus.getNetSalary() - noBonus.getNetSalary();
            assertTrue(diff > 0, "Chênh lệch phải dương");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 4: Tax deduction
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Test 4 – Tax deduction")
    class TaxDeductionTest {

        @Test
        @DisplayName("tax 10% FULLTIME: taxAmount = gross × 0.10")
        void fulltimeTax10Percent() {
            SalaryCalculator calc = SalaryCalculator
                    .forFulltime(BASE_10M, 5, 0)
                    .calculate();

            double expectedTax = Math.round(calc.getGrossSalary() * 0.10);
            assertEquals(expectedTax, calc.getTaxAmount(), DELTA,
                    "FULLTIME tax = gross × 10%");
        }

        @Test
        @DisplayName("tax 5% PARTTIME: taxAmount = gross × 0.05")
        void parttimeTax5Percent() {
            SalaryCalculator calc = SalaryCalculator
                    .forParttime(BASE_5M, 0, 0)
                    .calculate();

            double expectedTax = Math.round(calc.getGrossSalary() * 0.05);
            assertEquals(expectedTax, calc.getTaxAmount(), DELTA,
                    "PARTTIME tax = gross × 5%");
        }

        @Test
        @DisplayName("net = gross − tax (luôn đúng)")
        void netEqualsGrossMinusTax() {
            SalaryCalculator calc = new SalaryCalculator(12_000_000, 8, 1, 0.15)
                    .calculate();

            assertEquals(calc.getGrossSalary() - calc.getTaxAmount(),
                    calc.getNetSalary(), DELTA,
                    "Net phải luôn = Gross − Tax");
        }

        @Test
        @DisplayName("taxRate = 0 → taxAmount = 0, net = gross")
        void zeroTaxRate() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 5, 1, 0.0)
                    .calculate();

            assertEquals(0, calc.getTaxAmount(), DELTA);
            assertEquals(calc.getGrossSalary(), calc.getNetSalary(), DELTA,
                    "Thuế 0% → net = gross");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 5: No overtime
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Test 5 – No overtime")
    class NoOvertimeTest {

        @Test
        @DisplayName("OT=0 → overtimePay = 0")
        void noOvertimePayIsZero() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 0, 0.10)
                    .calculate();

            assertEquals(0, calc.getOvertimePay(), DELTA,
                    "Không OT → overtimePay = 0");
        }

        @Test
        @DisplayName("No OT, no absence → gross = base + bonus")
        void noOtNoAbsenceGrosEqualsBasePlusBonus() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 0, 0.10)
                    .calculate();

            double expectedBonus = Math.round(BASE_10M * 0.05);
            assertEquals(BASE_10M + expectedBonus, calc.getGrossSalary(), DELTA,
                    "Không OT, không absent → gross = base + bonus");
        }

        @Test
        @DisplayName("No OT, có absence → gross = base − absence")
        void noOtWithAbsence() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 2, 0.10)
                    .calculate();

            double expectedAbsence = Math.round(BASE_10M / 22.0 * 2);
            assertEquals(BASE_10M - expectedAbsence, calc.getGrossSalary(), DELTA,
                    "Không OT, có absent → gross = base − absence (no bonus)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // BONUS TESTS: Validation & Edge cases
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Validation & Edge cases")
    class ValidationTest {

        @Test
        @DisplayName("baseSalary âm → IllegalArgumentException")
        void negativeBaseSalaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalaryCalculator(-1, 0, 0, 0.10),
                    "Base salary âm phải throw exception");
        }

        @Test
        @DisplayName("overtimeHours âm → IllegalArgumentException")
        void negativeOvertimeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalaryCalculator(BASE_10M, -5, 0, 0.10),
                    "OT hours âm phải throw exception");
        }

        @Test
        @DisplayName("absenceDays âm → IllegalArgumentException")
        void negativeAbsenceThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalaryCalculator(BASE_10M, 0, -1, 0.10),
                    "Absence days âm phải throw exception");
        }

        @Test
        @DisplayName("taxRate > 1 → IllegalArgumentException")
        void taxRateOverOneThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalaryCalculator(BASE_10M, 0, 0, 1.5),
                    "Tax rate > 1 phải throw exception");
        }

        @Test
        @DisplayName("taxRate < 0 → IllegalArgumentException")
        void negativeTaxRateThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalaryCalculator(BASE_10M, 0, 0, -0.1),
                    "Tax rate < 0 phải throw exception");
        }

        @Test
        @DisplayName("Gọi getter trước calculate() → IllegalStateException")
        void getBeforeCalculateThrows() {
            SalaryCalculator calc = new SalaryCalculator(BASE_10M, 0, 0, 0.10);
            assertThrows(IllegalStateException.class, calc::getNetSalary,
                    "Phải gọi calculate() trước khi lấy kết quả");
        }

        @Test
        @DisplayName("baseSalary = 0 → tất cả = 0")
        void zeroBaseSalary() {
            SalaryCalculator calc = new SalaryCalculator(0, 10, 0, 0.10)
                    .calculate();

            assertEquals(0, calc.getOvertimePay(), DELTA);
            assertEquals(0, calc.getAbsenceDeduction(), DELTA);
            assertEquals(0, calc.getAttendanceBonus(), DELTA);
            assertEquals(0, calc.getGrossSalary(), DELTA);
            assertEquals(0, calc.getTaxAmount(), DELTA);
            assertEquals(0, calc.getNetSalary(), DELTA);
        }

        @Test
        @DisplayName("Factory forFulltime() dùng đúng tax rate 10%")
        void factoryFulltimeUsesCorrectTaxRate() {
            SalaryCalculator calc = SalaryCalculator.forFulltime(BASE_10M, 0, 0);
            assertEquals(0.10, calc.getTaxRate(), 0.001);
        }

        @Test
        @DisplayName("Factory forParttime() dùng đúng tax rate 5%")
        void factoryParttimeUsesCorrectTaxRate() {
            SalaryCalculator calc = SalaryCalculator.forParttime(BASE_5M, 0, 0);
            assertEquals(0.05, calc.getTaxRate(), 0.001);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Integration-style: end-to-end calculation scenarios
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("End-to-end scenarios")
    class EndToEndTest {

        @Test
        @DisplayName("Scenario: FULLTIME 12M, OT=15h, no absence → full calculation")
        void fullScenarioFulltime() {
            double base = 12_000_000;
            int otHours = 15;
            int absent = 0;
            double taxRate = 0.10;

            SalaryCalculator calc = new SalaryCalculator(base, otHours, absent, taxRate)
                    .calculate();

            // Manual calculation
            double otPay = Math.round(base / 22.0 / 8 * otHours * 1.5);
            double absDeduct = 0;
            double bonus = Math.round(base * 0.05);
            double gross = base + otPay - absDeduct + bonus;
            double tax = Math.round(gross * taxRate);
            double net = gross - tax;

            assertEquals(otPay,     calc.getOvertimePay(),      DELTA);
            assertEquals(absDeduct, calc.getAbsenceDeduction(), DELTA);
            assertEquals(bonus,     calc.getAttendanceBonus(),  DELTA);
            assertEquals(gross,     calc.getGrossSalary(),      DELTA);
            assertEquals(tax,       calc.getTaxAmount(),        DELTA);
            assertEquals(net,       calc.getNetSalary(),        DELTA);
        }

        @Test
        @DisplayName("Scenario: PARTTIME 6M, OT=5h, absent=2 → full calculation")
        void fullScenarioParttime() {
            double base = 6_000_000;
            int otHours = 5;
            int absent = 2;
            double taxRate = 0.05;

            SalaryCalculator calc = new SalaryCalculator(base, otHours, absent, taxRate)
                    .calculate();

            double otPay = Math.round(base / 22.0 / 8 * otHours * 1.5);
            double absDeduct = Math.round(base / 22.0 * absent);
            double bonus = 0; // absent > 0
            double gross = base + otPay - absDeduct + bonus;
            double tax = Math.round(gross * taxRate);
            double net = gross - tax;

            assertEquals(otPay,     calc.getOvertimePay(),      DELTA);
            assertEquals(absDeduct, calc.getAbsenceDeduction(), DELTA);
            assertEquals(bonus,     calc.getAttendanceBonus(),  DELTA);
            assertEquals(gross,     calc.getGrossSalary(),      DELTA);
            assertEquals(tax,       calc.getTaxAmount(),        DELTA);
            assertEquals(net,       calc.getNetSalary(),        DELTA);
        }

        @Test
        @DisplayName("toString() hiển thị kết quả sau calculate()")
        void toStringAfterCalculate() {
            SalaryCalculator calc = SalaryCalculator.forFulltime(BASE_10M, 5, 1)
                    .calculate();

            String result = calc.toString();
            assertTrue(result.contains("SalaryCalculator{"),
                    "toString phải có tên class");
            assertFalse(result.contains("not calculated"),
                    "Sau calculate() không được hiển thị 'not calculated'");
        }

        @Test
        @DisplayName("toString() trước calculate() hiển thị chưa tính")
        void toStringBeforeCalculate() {
            SalaryCalculator calc = SalaryCalculator.forFulltime(BASE_10M, 5, 1);
            assertTrue(calc.toString().contains("not calculated"));
        }
    }
}
