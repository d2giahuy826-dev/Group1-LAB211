package model;

import model.enums.PayrollStatus;

public class PayrollRun extends BaseEntity {
    private int month;
    private int year;
    private PayrollStatus status;

    public PayrollRun() { super(); }

    @Override
    public String toCsvLine() {
        return String.format("%s,%d,%d,%s", id, month, year, status);
    }

    @Override
    public void fromCsvLine(String line) {
        String[] parts = line.split(",");
        this.id = parts[0];
        this.month = Integer.parseInt(parts[1]);
        this.year = Integer.parseInt(parts[2]);
        this.status = PayrollStatus.valueOf(parts[3]);
    }

    // --- GETTER & SETTER ---
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
}