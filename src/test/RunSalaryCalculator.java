package test;
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

        List<Employee> employees = empRepo.loadAll();
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
        Employee selected = selectEmployee(employees);
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
            System.out.println("      LICH SU LUONG");
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

        AttendanceRepository attendanceRepo = new AttendanceRepository();
        List<AttendanceRecord> attendanceRecords = attendanceRepo.findByEmployeeId(selected.getId());

        if (!attendanceRecords.isEmpty()) {
            System.out.println("\n=======================================");
            System.out.println("      LICH SU CHAM CONG");
            System.out.println("=======================================");
            for (int i = 0; i < attendanceRecords.size(); i++) {
                AttendanceRecord record = attendanceRecords.get(i);
                System.out.printf("%d. Thang %d/%d - WorkDays: %d - Absence: %d - OT: %d%n",
                        i + 1,
                        record.getMonth(),
                        record.getYear(),
                        record.getWorkDays(),
                        record.getAbsenceDays(),
                        record.getOvertimeHours()
                );
            }
        } else {
            System.out.println("\n[WARN] Khong co du lieu cham cong cho nhan vien nay.");
        }

        int selectedMonth = selectMonth(attendanceRecords);
        if (selectedMonth == 0) {
            showAnnualAverage(selected, attendanceRecords);
        } else {
            AttendanceRecord attendance = attendanceRecords.stream()
                    .filter(r -> r.getMonth() == selectedMonth)
                    .findFirst()
                    .orElse(null);
            if (attendance != null) {
                showMonthlySalary(selected, attendance);
                showAnnualAverage(selected, attendanceRecords);
            } else {
                System.out.println("[ERROR] Khong co du lieu cham cong cho thang da chon!");
            }
        }
    }

    private static Employee selectEmployee(List<Employee> employees) {
        while (true) {
            System.out.println("\n=======================================");
            System.out.print("Nhap so thu tu nhan vien (hoac nhap ID): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("[ERROR] Vui long nhap so thu tu hoac ID cua nhan vien.");
                continue;
            }

            Employee selected = null;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < employees.size()) {
                    selected = employees.get(index);
                }
            } catch (NumberFormatException e) {
                selected = employees.stream()
                        .filter(emp -> emp.getId().equalsIgnoreCase(input))
                        .findFirst()
                        .orElse(null);
            }

            if (selected != null) {
                return selected;
            }

            System.out.println("[ERROR] Khong tim thay nhan vien! Vui long thu lai.");
        }
    }

    private static int selectMonth(List<AttendanceRecord> attendanceRecords) {
        while (true) {
            System.out.println("\n---------------------------------------");
            System.out.println("Chon thang de tinh luong hoac nhap 0 de tinh trung binh ca nam");
            System.out.print("Thang (0-12): ");
            int month = getIntInput();

            if (month == 0) {
                return 0;
            }
            if (month >= 1 && month <= 12) {
                if (attendanceRecords.stream().anyMatch(r -> r.getMonth() == month)) {
                    return month;
                }
                System.out.println("[ERROR] Khong co du lieu cham cong cho thang " + month + ". Vui long chon lai.");
            } else {
                System.out.println("[ERROR] Thang phai trong khoang 0-12. Vui long nhap lai.");
            }
        }
    }

    private static void showMonthlySalary(Employee selected, AttendanceRecord attendance) {
        SalaryCalculator calc = selected.getEmpType() == EmpType.FULLTIME
                ? SalaryCalculator.forFulltime(selected.getBaseSalary(), attendance.getOvertimeHours(), attendance.getAbsenceDays())
                : SalaryCalculator.forParttime(selected.getBaseSalary(), attendance.getOvertimeHours(), attendance.getAbsenceDays());
        calc.calculate();

        System.out.println("\n=======================================");
        System.out.println("      KET QUA TINH LUONG THANG " + attendance.getMonth() + "/" + attendance.getYear());
        System.out.println("=======================================");
        System.out.println("  Base salary: " + formatMoney(selected.getBaseSalary()));
        System.out.println("  Overtime hours: " + attendance.getOvertimeHours());
        System.out.println("  Absence days: " + attendance.getAbsenceDays());
        System.out.println("  Overtime pay: " + formatMoney(calc.getOvertimePay()));
        System.out.println("  Absence deduction: " + formatMoney(calc.getAbsenceDeduction()));
        System.out.println("  Attendance bonus: " + formatMoney(calc.getAttendanceBonus()));
        System.out.println("  Gross salary: " + formatMoney(calc.getGrossSalary()));
        System.out.println("  Tax (" + ((long) (selected.getTaxRate() * 100)) + "%): " + formatMoney(calc.getTaxAmount()));
        System.out.println("  Net salary: " + formatMoney(calc.getNetSalary()));
        System.out.println("  Tong so tien nhan duoc: " + formatMoney(calc.getNetSalary()) + " VND");
        System.out.println("=======================================");
    }

    private static void showAnnualAverage(Employee selected, List<AttendanceRecord> attendanceRecords) {
        if (attendanceRecords.isEmpty()) {
            System.out.println("\n[WARN] Khong the tinh trung binh nam vi khong co du lieu cham cong.");
            return;
        }

        double totalNet = 0;
        int count = 0;
        for (AttendanceRecord record : attendanceRecords) {
            SalaryCalculator calc = selected.getEmpType() == EmpType.FULLTIME
                    ? SalaryCalculator.forFulltime(selected.getBaseSalary(), record.getOvertimeHours(), record.getAbsenceDays())
                    : SalaryCalculator.forParttime(selected.getBaseSalary(), record.getOvertimeHours(), record.getAbsenceDays());
            calc.calculate();
            totalNet += calc.getNetSalary();
            count++;
        }

        long averageNet = Math.round(totalNet / count);
        long totalNetRounded = Math.round(totalNet);

        System.out.println("\n=======================================");
        System.out.println("      TRUNG BINH LUONG THEO NAM");
        System.out.println("=======================================");
        System.out.println("  So thang duoc tinh: " + count);
        System.out.println("  Trung binh net moi thang: " + df.format(averageNet) + " VND");
        System.out.println("  Tong net ca nam: " + df.format(totalNetRounded) + " VND");
        System.out.println("=======================================\n");
    }

    private static String formatMoney(double value) {
        return df.format(Math.round(value));
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
