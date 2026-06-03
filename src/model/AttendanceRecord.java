package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

public class AttendanceRecord extends BaseEntity {
    private String employeeId;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;

    public AttendanceRecord() { super(); }

    public double calculateDailyHours() {
        if (checkIn != null && checkOut != null) {
            Duration duration = Duration.between(checkIn, checkOut);
            return duration.toMinutes() / 60.0;
        }
        return 0;
    }

    @Override
    public String toCsvLine() {
        return String.format("%s,%s,%s,%s,%s", 
                id, employeeId, date, checkIn, checkOut);
    }

    @Override
    public void fromCsvLine(String line) {
        String[] parts = line.split(",");
        this.id = parts[0];
        this.employeeId = parts[1];
        this.date = LocalDate.parse(parts[2]);
        this.checkIn = LocalTime.parse(parts[3]);
        this.checkOut = LocalTime.parse(parts[4]);
    }

    // --- GETTER & SETTER ---
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }

    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }
}