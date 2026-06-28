package view;

import repository.PayrollEntryRepository;
import repository.EmployeeRepository;
import repository.LeaveRequestRepository;
import model.PayrollEntry;
import model.Employee;
import model.LeaveRequest;

import java.util.List;

public class ReportView {

    private final PayrollEntryRepository payrollRepo =
            new PayrollEntryRepository("data/payroll_entries.csv");
    private final EmployeeRepository employeeRepo = new EmployeeRepository();
    private final LeaveRequestRepository leaveRequestRepo = new LeaveRequestRepository();

    public void show() {
        System.out.println("\n===== BAO CAO TONG KET THANG =====");

        List<PayrollEntry> entries = payrollRepo.findAll();
        List<Employee> employees = employeeRepo.loadAll();
        List<LeaveRequest> leaveRequests = leaveRequestRepo.loadAll();

        long totalNetPay = 0;
        for (PayrollEntry entry : entries) {
            totalNetPay += entry.getNetSalary();
        }

        System.out.println("Tong net pay: " + totalNetPay);
        System.out.println("So nhan vien: " + employees.size());
        System.out.println("So don phep: " + leaveRequests.size());
    }
}
