package model;

/**
 * Trạng thái của đơn xin nghỉ phép.
 */
public enum LeaveStatus {

    /** Đang chờ duyệt */
    PENDING,

    /** Đã được duyệt */
    APPROVED,

    /** Bị từ chối */
    REJECTED;

    /**
     * Kiểm tra đơn đã được xử lý (không còn ở trạng thái chờ).
     */
    public boolean isProcessed() {
        return this != PENDING;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public static LeaveStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LeaveStatus cannot be null or blank.");
        }
        return LeaveStatus.valueOf(value.trim().toUpperCase());
    }
}
