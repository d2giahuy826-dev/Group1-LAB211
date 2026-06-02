package model;

/**
 * Loại nghỉ phép.
 */
public enum LeaveType {

    /** Nghỉ phép năm (annual leave) – tối đa 12 ngày/năm */
    ANNUAL,

    /** Nghỉ ốm (sick leave) – tối đa 6 ngày/năm */
    SICK;

    /**
     * Trả về số ngày phép tối đa cho loại nghỉ này.
     */
    public int getMaxDaysPerYear() {
        return this == ANNUAL ? 12 : 6;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public static LeaveType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LeaveType cannot be null or blank.");
        }
        return LeaveType.valueOf(value.trim().toUpperCase());
    }
}
