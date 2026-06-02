import model.SalaryCalculator;
import java.text.DecimalFormat;

public class RunSalaryCalculator {
    public static void main(String[] args) {
        SalaryCalculator sc = SalaryCalculator.forFulltime(12000000, 10, 0).calculate();
        System.out.println(sc.toString());

        DecimalFormat df = new DecimalFormat("#,###");

        // Format helper: round to nearest integer and apply grouping
        java.util.function.Function<Double, String> fmt = v -> df.format(Math.round(v));

        System.out.println("Overtime pay: " + fmt.apply(sc.getOvertimePay()));
        System.out.println("Absence deduction: " + fmt.apply(sc.getAbsenceDeduction()));
        System.out.println("Attendance bonus: " + fmt.apply(sc.getAttendanceBonus()));

        // Round gross/net to nearest thousand, then format
        long grossRounded = Math.round(sc.getGrossSalary() / 1000.0) * 1000;
        long netRounded = Math.round(sc.getNetSalary() / 1000.0) * 1000;

        System.out.println("Gross salary: " + df.format(grossRounded));
        System.out.println("Tax amount: " + fmt.apply(sc.getTaxAmount()));
        System.out.println("Net salary: " + df.format(netRounded));
    }
}
