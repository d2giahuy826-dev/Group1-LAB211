package model;

/**
 * Trạng thái xử lý của một payroll entry (dòng lương cá nhân).
 */
public enum PayStatus {

    /** Chưa xử lý – chờ tính lương */
    PENDING,

    /** Đã tính và xử lý xong */
    PROCESSED;

    /**
     * Kiểm tra entry đã được xử lý.
     */
    public boolean isProcessed() {
        return this == PROCESSED;
    }

    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public static PayStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PayStatus cannot be null or blank.");
        }
        return PayStatus.valueOf(value.trim().toUpperCase());
    }
}
