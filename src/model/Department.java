package model;

public class Department extends BaseEntity {

    private String name;
    private String managerId;
    private int totalEmployees;

    public Department() {
    }

    public Department(String id, String name, String managerId, int totalEmployees) {
        super(id);
        setName(name);
        setManagerId(managerId);
        setTotalEmployees(totalEmployees);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateRequired(name, "Department name");
        this.name = name.trim();
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        validateRequired(managerId, "Manager ID");
        this.managerId = managerId.trim();
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        validateNonNegative(totalEmployees, "Total employees");
        this.totalEmployees = totalEmployees;
    }

    @Override
    public String toCsvLine() {
        return id + "," + name + "," + managerId + "," + totalEmployees;
    }

    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",");

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid Department CSV format.");
        }

        setId(parts[0]);
        setName(parts[1]);
        setManagerId(parts[2]);
        setTotalEmployees(Integer.parseInt(parts[3]));
    }

    @Override
    public String toString() {
        return "Department{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", managerId='" + managerId + '\'' +
                ", totalEmployees=" + totalEmployees +
                '}';
    }
}