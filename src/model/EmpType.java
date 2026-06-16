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
        return this == FULLTIME ? 0.10 : 0.05;
    }

    /**chuyển một chuỗi (String) thành giá trị enum
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public EmpType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("EmpType cannot be null or blank.");
        }

        String input = value.trim().toUpperCase();

        for (EmpType type : EmpType.values()) {
            if (type.name().equals(input)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid EmpType: " + value);
    }
}