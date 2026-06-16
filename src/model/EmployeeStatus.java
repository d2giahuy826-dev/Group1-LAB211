package model;

/**
 * Trạng thái hoạt động của nhân viên.
 */
public enum EmployeeStatus {

    /** Nhân viên đang làm việc */
    ACTIVE,

    /** Nhân viên đã nghỉ việc */
    INACTIVE;

    /**
     * Kiểm tra nhân viên còn đang làm.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public EmployeeStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("EmployeeStatus cannot be null or blank.");
        }
        return EmployeeStatus.valueOf(value.trim().toUpperCase());
    }
}
