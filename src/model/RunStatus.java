package model;

/**
 * Trạng thái của một payroll run (đợt chạy lương toàn công ty).
 */
public enum RunStatus {

    /** Đang chờ hoặc đang chạy */
    PENDING,

    /** Đã hoàn thành */
    COMPLETED;

    /**
     * Kiểm tra đợt chạy lương đã hoàn thành.
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public static RunStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RunStatus cannot be null or blank.");
        }
        return RunStatus.valueOf(value.trim().toUpperCase());
    }
}
