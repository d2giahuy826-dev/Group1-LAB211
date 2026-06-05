package model;

public class LeaveBalance extends BaseEntity {

    private String employeeId;
    private int annualRemaining;
    private int sickRemaining;
    private int version;

    public LeaveBalance() {
        super();
    }

    @Override
    public String toCsvLine() {
        return String.format("%s,%s,%d,%d,%d",
                id,
                employeeId,
                annualRemaining,
                sickRemaining,
                version);
    }

    @Override
    public void fromCsvLine(String line) {

        String[] parts = line.split(",");

        this.id = parts[0];
        this.employeeId = parts[1];
        this.annualRemaining = Integer.parseInt(parts[2]);
        this.sickRemaining = Integer.parseInt(parts[3]);
        this.version = Integer.parseInt(parts[4]);
    }

    public void deductLeave(int days) {
        annualRemaining -= days;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public int getAnnualRemaining() {
        return annualRemaining;
    }

    public int getSickRemaining() {
        return sickRemaining;
    }

    public int getVersion() {
        return version;
    }
}