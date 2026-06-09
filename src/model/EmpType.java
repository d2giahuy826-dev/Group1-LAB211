package model;

/**
 * Loại hợp đồng của nhân viên.
 */
public enum EmpType {

    /** Nhân viên toàn thời gian – thuế 10% */
    FULLTIME,

    /** Nhân viên bán thời gian – thuế 5% */
    PARTTIME;

    /**
     * Trả về tax rate mặc định theo loại nhân viên.
     */
    public double getDefaultTaxRate() {
        return this == FULLTIME
                ? SalaryCalculator.DEFAULT_FULLTIME_TAX_RATE
                : SalaryCalculator.DEFAULT_PARTTIME_TAX_RATE;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     * Ném IllegalArgumentException nếu không hợp lệ.
     */
    public static EmpType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("EmpType cannot be null or blank.");
        }
        return EmpType.valueOf(value.trim().toUpperCase());
    }
}
