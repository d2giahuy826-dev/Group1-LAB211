import model.SalaryCalculator;

public class RunSalaryCalculator {
    public static void main(String[] args) {
        SalaryCalculator sc = SalaryCalculator.forFulltime(12000000, 10, 0).calculate();
        System.out.println(sc.toString());
        System.out.println("Overtime pay: " + sc.getOvertimePay());
        System.out.println("Absence deduction: " + sc.getAbsenceDeduction());
        System.out.println("Attendance bonus: " + sc.getAttendanceBonus());
        System.out.println("Gross salary: " + sc.getGrossSalary());
        System.out.println("Tax amount: " + sc.getTaxAmount());
        System.out.println("Net salary: " + sc.getNetSalary());
    }
}
