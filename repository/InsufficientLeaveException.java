package exception;

/**
 * InsufficientLeaveException – Ném khi nhân viên không đủ ngày phép.
 *
 * Xảy ra trong LeaveBalanceRepository.deductLeave() khi:
 *   - annualRemaining < daysRequested  (nghỉ phép năm)
 *   - sickRemaining   < daysRequested  (nghỉ ốm)
 *
 * Kịch bản race condition:
 *   Thread A: đọc LeaveBalance EMP001 → annualRemaining = 2
 *   Thread B: đọc LeaveBalance EMP001 → annualRemaining = 2
 *   Thread A: trừ 2 ngày → remaining = 0 → ghi thành công
 *   Thread B: cố trừ 2 ngày → remaining thực tế = 0 → throw InsufficientLeaveException
 *
 * Cách xử lý khi bắt:
 *   - Trả về lỗi cho HR / người duyệt đơn
 *   - Đặt LeaveRequest.status = REJECTED
 *   - Log để audit
 */
public class InsufficientLeaveException extends RuntimeException {

    /** ID nhân viên bị thiếu phép */
    private final String empId;

    /** Loại phép (ANNUAL / SICK) */
    private final String leaveType;

    /** Số ngày còn lại thực tế */
    private final int remaining;

    /** Số ngày đang yêu cầu */
    private final int requested;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * @param empId     ID nhân viên
     * @param leaveType loại phép ("ANNUAL" hoặc "SICK")
     * @param remaining số ngày còn lại trong balance
     * @param requested số ngày nhân viên yêu cầu
     */
    public InsufficientLeaveException(String empId, String leaveType,
                                      int remaining, int requested) {
        super(String.format(
            "Insufficient leave for employee '%s': "
            + "requested %d %s leave day(s) but only %d remaining. "
            + "Please reduce request or choose a different leave type.",
            empId, requested, leaveType, remaining));
        this.empId     = empId;
        this.leaveType = leaveType;
        this.remaining = remaining;
        this.requested = requested;
    }

    /**
     * Constructor với message tuỳ chỉnh.
     */
    public InsufficientLeaveException(String message, String empId,
                                      String leaveType, int remaining, int requested) {
        super(message);
        this.empId     = empId;
        this.leaveType = leaveType;
        this.remaining = remaining;
        this.requested = requested;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getEmpId()     { return empId;     }
    public String getLeaveType() { return leaveType; }
    public int    getRemaining() { return remaining; }
    public int    getRequested() { return requested; }

    /**
     * Số ngày thiếu (để HR biết cần bao nhiêu ngày nữa).
     */
    public int getShortfall() {
        return Math.max(0, requested - remaining);
    }
}
