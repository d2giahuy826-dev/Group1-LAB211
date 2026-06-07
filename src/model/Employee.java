package model;

import java.time.LocalDate;

/**
 * Employee – Nhân viên trong hệ thống Payroll.
 *
 * CSV columns (8):
 *   empId, fullName, deptId, empType, baseSalary, taxRate, joinDate, status
 */
public class Employee extends BaseEntity {

    private String fullName;
    private String deptId;
    private EmpType empType;
    private double baseSalary;
    private double taxRate;
    private LocalDate joinDate;
    private EmployeeStatus status;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Employee() {}

    public Employee(String empId, String fullName, String deptId,
                    EmpType empType, double baseSalary, double taxRate,
                    LocalDate joinDate, EmployeeStatus status) {
        super(empId);
        setFullName(fullName);
        setDeptId(deptId);
        setEmpType(empType);
        setBaseSalary(baseSalary);
        setTaxRate(taxRate);
        setJoinDate(joinDate);
        setStatus(status);
    }

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 8 cột.
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id,
                fullName,
                deptId,
                empType.name(),
                String.valueOf((long) baseSalary),
                String.valueOf(taxRate),
                joinDate.toString(),
                status.name()
        );
    }

    /**
     * Parse từ dòng CSV (8 cột).
     * Format: empId,fullName,deptId,empType,baseSalary,taxRate,joinDate,status
     */
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        // Tách an toàn: fullName có thể chứa space nhưng không chứa dấu phẩy
        String[] parts = csvLine.split(",", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException(
                    "Invalid Employee CSV: expected 8 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }

        setId(parts[0].trim());
        setFullName(parts[1].trim());
        setDeptId(parts[2].trim());
        setEmpType(EmpType.fromString(parts[3].trim()));
        setBaseSalary(Double.parseDouble(parts[4].trim()));
        setTaxRate(Double.parseDouble(parts[5].trim()));
        setJoinDate(LocalDate.parse(parts[6].trim()));
        setStatus(EmployeeStatus.fromString(parts[7].trim()));
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        validateRequired(fullName, "Full name");
        this.fullName = fullName.trim();
    }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) {
        validateRequired(deptId, "Department ID");
        this.deptId = deptId.trim();
    }

    public EmpType getEmpType() { return empType; }
    public void setEmpType(EmpType empType) {
        if (empType == null) throw new IllegalArgumentException("EmpType is required.");
        this.empType = empType;
    }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) {
        validateNonNegative(baseSalary, "Base salary");
        this.baseSalary = baseSalary;
    }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) {
        if (taxRate < 0 || taxRate > 1)
            throw new IllegalArgumentException("Tax rate must be between 0 and 1.");
        this.taxRate = taxRate;
    }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) {
        if (joinDate == null) throw new IllegalArgumentException("Join date is required.");
        this.joinDate = joinDate;
    }

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        this.status = status;
    }

    // ─── Convenience ──────────────────────────────────────────────────────────

    /** Trả về true nếu nhân viên đang hoạt động. */
    public boolean isActive() {
        return status != null && status.isActive();
    }

    @Override
    public String toString() {
        return "Employee{id='" + id + "', fullName='" + fullName +
               "', deptId='" + deptId + "', empType=" + empType +
               ", baseSalary=" + baseSalary + ", status=" + status + "}";
    }
}
