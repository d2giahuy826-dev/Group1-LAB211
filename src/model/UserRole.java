package model;
 
/**
 * Vai trò người dùng trong hệ thống.
 */
public enum UserRole {
 
    /** Nhân viên thường */
    EMPLOYEE,
 
    /** Nhân sự (duyệt nghỉ phép, quản lý nhân viên/phòng ban) */
    HR_STAFF,
 
    /** Nhân viên kế toán lương (chạy bảng lương, xem báo cáo) */
    PAYROLL_STAFF;
 
    /**
     * Parse chuỗi từ CSV (không phân biệt hoa/thường).
     */
    public static UserRole fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserRole cannot be null or blank.");
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}