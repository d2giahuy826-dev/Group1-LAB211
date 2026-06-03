package model;

import model.enums.PayrollStatus;

public class PayrollEntry extends BaseEntity {
    private String employeeId;
    private String runId;
    private double totalSalary;
    private PayrollStatus status;
    private int version; // Chống double payment (Optimistic Locking)

    public PayrollEntry() { super(); }

    @Override
    public String toCsvLine() {
        return String.format("%s,%s,%s,%.2f,%s,%d", 
                id, employeeId, runId, totalSalary, status, version);
    }

    @Override
    public void fromCsvLine(String line) {
        String[] parts = line.split(",");
        this.id = parts[0];
        this.employeeId = parts[1];
        this.runId = parts[2];
        this.totalSalary = Double.parseDouble(parts[3]);
        this.status = PayrollStatus.valueOf(parts[4]);
        this.version = Integer.parseInt(parts[5]);
    }

    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public double getTotalSalary() { return totalSalary; }
    public void setTotalSalary(double totalSalary) { this.totalSalary = totalSalary; }
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}