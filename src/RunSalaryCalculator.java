import model.*;
import repository.*;
import java.text.DecimalFormat;
import java.util.*;

public class RunSalaryCalculator {
    private static final DecimalFormat df = new DecimalFormat("#,###");
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // 1. Load repositories
        EmployeeRepository empRepo = new EmployeeRepository();
        PayrollEntryRepository payrollRepo = new PayrollEntryRepository("data/payroll_entries.csv");

        List<Employee> employees = empRepo.getAll();
        if (employees.isEmpty()) {
            System.out.println("[ERROR] Khong co nhan vien trong he thong!");
            return;
        }

        // 2. Display list of employees
        System.out.println("\n=======================================");
        System.out.println("      DANH SACH NHAN VIEN");
        System.out.println("=======================================");
        for (int i = 0; i < employees.size(); i++) {
            Employee emp = employees.get(i);
            System.out.printf("%d. [%s] %s - %s - Base: %s VND%n",
                    i + 1,
                    emp.getId(),
                    emp.getFullName(),
                    emp.getEmpType(),
                    df.format((long) emp.getBaseSalary())
            );
        }

        // 3. User select employee
        System.out.println("\n=======================================");
        System.out.print("Nhap so thu tu nhan vien (hoac nhap ID): ");
        String input = scanner.nextLine().trim();

        Employee selected = null;
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < employees.size()) {
                selected = employees.get(index);
            }
        } catch (NumberFormatException e) {
            // Try find by ID
            selected = employees.stream()
                    .filter(emp -> emp.getId().equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);
        }

        if (selected == null) {
            System.out.println("[ERROR] Khong tim thay nhan vien!");
            return;
        }

        System.out.printf("\n[OK] Chon nhan vien: %s (%s)%n", selected.getFullName(), selected.getId());

        // 4. Display payroll history of this employee
        List<PayrollEntry> empPayrolls = new ArrayList<>();
        List<PayrollEntry> allPayrolls = payrollRepo.findAll();
        for (PayrollEntry p : allPayrolls) {
            if (p.getEmpId().equals(selected.getId())) {
                empPayrolls.add(p);
            }
        }

        if (!empPayrolls.isEmpty()) {
            System.out.println("\n=======================================");
            System.out.println("      LỊCH SỬ LƯƠNG");
            System.out.println("=======================================");
            for (int i = 0; i < empPayrolls.size(); i++) {
                PayrollEntry p = empPayrolls.get(i);
                System.out.printf("%d. Thang %d/%d - Net: %s VND - Status: %s%n",
                        i + 1,
                        p.getMonth(),
                        p.getYear(),
                        df.format(p.getNetSalary()),
                        p.getStatus()
                );
            }
        }

        // 5. User input OT hours and absence days
        System.out.println("\n=======================================");
        System.out.println("      TIN LUONG");
        System.out.println("=======================================");

        System.out.print("Nhap so gio lam them (OT hours): ");
        int otHours = getIntInput();

        System.out.print("Nhap so ngay nghi (absence days): ");
        int absenceDays = getIntInput();

        // 6. Calculate salary
        SalaryCalculator calc;
        if (selected.getEmpType() == EmpType.FULLTIME) {
            calc = SalaryCalculator.forFulltime(
                    selected.getBaseSalary(),
                    otHours,
                    absenceDays
            ).calculate();
        } else {
            calc = SalaryCalculator.forParttime(
                    selected.getBaseSalary(),
                    otHours,
                    absenceDays
            ).calculate();
        }

        // 7. Display result
        System.out.println("\n=======================================");
        System.out.println("      KET QUA TIN LUONG");
        System.out.println("=======================================");
        System.out.println(calc.toString());

        java.util.function.Function<Double, String> fmt = v -> df.format(Math.round(v));

        System.out.println("\nChi tiet:");
        System.out.println("  Base salary: " + fmt.apply(selected.getBaseSalary()));
        System.out.println("  Overtime pay: " + fmt.apply(calc.getOvertimePay()));
        System.out.println("  Absence deduction: " + fmt.apply(calc.getAbsenceDeduction()));
        System.out.println("  Attendance bonus: " + fmt.apply(calc.getAttendanceBonus()));

        long grossRounded = Math.round(calc.getGrossSalary() / 1000.0) * 1000;
        long netRounded = Math.round(calc.getNetSalary() / 1000.0) * 1000;

        System.out.println("  Gross salary: " + df.format(grossRounded));
        System.out.println("  Tax (" + ((long)(selected.getTaxRate() * 100)) + "%): " + fmt.apply(calc.getTaxAmount()));
        System.out.println("  Net salary: " + df.format(netRounded));
        System.out.println("=======================================\n");
    }

    private static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.print("❌ Nhập sai! Vui lòng nhập số nguyên: ");
            return getIntInput();
        }
    }
}
