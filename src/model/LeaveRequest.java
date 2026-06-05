package model;

import java.time.LocalDate;

public class LeaveRequest extends BaseEntity {

    private String employeeId;
    private LeaveType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveStatus status;

    public LeaveRequest() {
        super();
    }

    @Override
    public String toCsvLine() {

        return String.format("%s,%s,%s,%s,%s,%s",
                id,
                employeeId,
                type,
                startDate,
                endDate,
                status);
    }

    @Override
    public void fromCsvLine(String line) {

        String[] parts = line.split(",");

        this.id = parts[0];
        this.employeeId = parts[1];
        this.type = LeaveType.fromString(parts[2]);
        this.startDate = LocalDate.parse(parts[3]);
        this.endDate = LocalDate.parse(parts[4]);
        this.status = LeaveStatus.fromString(parts[5]);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LeaveType getType() {
        return type;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }
}