package model;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

/**
 * LeaveRequest – Đơn xin nghỉ phép của nhân viên.
 *
 * CSV columns (10):
 *   requestId, empId, leaveType, startDate, endDate,
 *   days, reason, status, approvedBy, createdAt
 *
 * LƯU Ý FIX: Model gốc chỉ parse 6 cột, thiếu: days, reason, approvedBy, createdAt.
 * Đã bổ sung đầy đủ để khớp với leave_requests.csv thực tế (10 cột).
 */
public class LeaveRequest extends BaseEntity {

    // ─── Fields ───────────────────────────────────────────────────────────────

    /** empId – khớp với cột "empId" trong CSV (DataGenerator dùng tên này) */
    private final DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofPattern("M/d/yyyy");
    private String empId;

    private LeaveType type;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Số ngày nghỉ (đã tính sẵn trong CSV, không tính lại) */
    private int days;

    /** Lý do nghỉ phép */
    private String reason;

    private LeaveStatus status;

    /**
     * ID người duyệt – rỗng ("") nếu status=PENDING.
     * Lưu dạng String để tránh parse lỗi khi CSV có cột trống.
     */
    private String approvedBy;

    /** Ngày tạo đơn (yyyy-MM-dd) */
    private String createdAt;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public LeaveRequest() {
        super();
    }

    public LeaveRequest(String requestId, String empId, LeaveType type,
                        LocalDate startDate, LocalDate endDate, int days,
                        String reason, LeaveStatus status,
                        String approvedBy, String createdAt) {
        super(requestId);
        setEmpId(empId);
        setType(type);
        setStartDate(startDate);
        setEndDate(endDate);
        setDays(days);
        setReason(reason);
        setStatus(status);
        setApprovedBy(approvedBy);
        setCreatedAt(createdAt);
    }

    // ─── CsvMappable ─────────────────────────────────────────────────────────

    /**
     * Xuất ra dòng CSV đúng thứ tự 10 cột.
     * approvedBy có thể rỗng nếu status=PENDING → để cột trống (không phải "null").
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                id,
                empId,
                type.name(),
                startDate.toString(),
                endDate.toString(),
                String.valueOf(days),
                escapeCsv(reason),
                status.name(),
                approvedBy == null ? "" : approvedBy,
                createdAt  == null ? "" : createdAt
        );
    }

    /**
     * Parse từ dòng CSV (10 cột).
     * Format: requestId,empId,leaveType,startDate,endDate,
     *         days,reason,status,approvedBy,createdAt
     *
     * FIX so với model gốc:
     *  - Dùng split(",", -1) để giữ cột rỗng (approvedBy khi PENDING)
     *  - Parse đủ 10 cột thay vì 6
     *  - Validate số cột
     */
    @Override
    public void fromCsvLine(String line) {
        validateRequired(line, "CSV line");

        // -1 giữ nguyên cột rỗng ở cuối (approvedBy, createdAt có thể trống)
        String[] parts = line.split(",", -1);

        if (parts.length != 10) {
            throw new IllegalArgumentException(
                "Invalid LeaveRequest CSV: expected 10 columns, got " + parts.length
                + " → [" + line + "]");
        }

        setId(parts[0].trim());
        setEmpId(parts[1].trim());
        setType(LeaveType.fromString(parts[2].trim()));
        setStartDate(LocalDate.parse(parts[3].trim(), dateFormatter));
        setEndDate(LocalDate.parse(parts[4].trim(), dateFormatter));
        setDays(Integer.parseInt(parts[5].trim()));
        setReason(parts[6].trim());
        setStatus(LeaveStatus.fromString(parts[7].trim()));
        setApprovedBy(parts[8].trim());   // "" nếu PENDING — không ném exception
        setCreatedAt(parts[9].trim());
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    /**
     * Kiểm tra đơn nghỉ phép đã được duyệt chưa.
     */
    public boolean isApproved() {
        return status == LeaveStatus.APPROVED;
    }

    /**
     * Kiểm tra đơn đang chờ xử lý.
     */
    public boolean isPending() {
        return status == LeaveStatus.PENDING;
    }

    /**
     * Duyệt đơn nghỉ phép – cập nhật status và approvedBy.
     *
     * @param approverId ID người duyệt (ví dụ: "EMP0001")
     */
    public void approve(String approverId) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                "Chỉ có thể duyệt đơn ở trạng thái PENDING. Trạng thái hiện tại: " + status);
        }
        this.status     = LeaveStatus.APPROVED;
        this.approvedBy = approverId;
    }

    /**
     * Từ chối đơn nghỉ phép.
     *
     * @param approverId ID người từ chối
     */
    public void reject(String approverId) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                "Chỉ có thể từ chối đơn ở trạng thái PENDING. Trạng thái hiện tại: " + status);
        }
        this.status     = LeaveStatus.REJECTED;
        this.approvedBy = approverId;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Escape dấu phẩy trong reason (nếu có) để không phá vỡ CSV.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    /** @deprecated dùng getEmpId() thay cho getEmployeeId() cho nhất quán với CSV */
    @Deprecated
    public String getEmployeeId() { return empId; }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) {
        validateRequired(empId, "Employee ID");
        this.empId = empId.trim();
    }

    public LeaveType getType() { return type; }
    public void setType(LeaveType type) {
        if (type == null) throw new IllegalArgumentException("LeaveType is required.");
        this.type = type;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) {
        if (startDate == null) throw new IllegalArgumentException("Start date is required.");
        this.startDate = startDate;
    }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) {
        if (endDate == null) throw new IllegalArgumentException("End date is required.");
        this.endDate = endDate;
    }

    public int getDays() { return days; }
    public void setDays(int days) {
        validateNonNegative(days, "Days");
        this.days = days;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason.trim();
    }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        this.status = status;
    }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy == null ? "" : approvedBy.trim();
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt == null ? "" : createdAt.trim();
    }

    @Override
    public String toString() {
        return "LeaveRequest{id='" + id + "', empId='" + empId
               + "', type=" + type + ", " + startDate + " → " + endDate
               + ", days=" + days + ", status=" + status + "}";
    }
}
