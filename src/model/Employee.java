package model;

public class Employee extends BaseEntity {

    private String name;
    private String departmentId;
    private double baseSalary;
    private String role;

    public Employee() {
    }

    public Employee(String id, String name, String departmentId,
                    double baseSalary, String role) {
        super(id);
        setName(name);
        setDepartmentId(departmentId);
        setBaseSalary(baseSalary);
        setRole(role);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateRequired(name, "Employee name");
        this.name = name.trim();
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        validateRequired(departmentId, "Department ID");
        this.departmentId = departmentId.trim();
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        validateNonNegative(baseSalary, "Base salary");
        this.baseSalary = baseSalary;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        validateRequired(role, "Role");
        this.role = role.trim();
    }

    @Override
    public String toCsvLine() {
        return id + "," + name + "," + departmentId + "," + baseSalary + "," + role;
    }

    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",");

        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid Employee CSV format.");
        }

        setId(parts[0]);
        setName(parts[1]);
        setDepartmentId(parts[2]);
        setBaseSalary(Double.parseDouble(parts[3]));
        setRole(parts[4]);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", baseSalary=" + baseSalary +
                ", role='" + role + '\'' +
                '}';
    }
}