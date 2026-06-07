package exception;

/**
 * DuplicatePaymentException – Ném khi phát hiện Double Payment (trả lương trùng).
 *
 * Đây là lỗi nghiêm trọng nhất trong hệ thống Payroll:
 * một nhân viên bị tính lương hai lần trong cùng một tháng.
 *
 * Kịch bản xảy ra (race condition):
 *   Thread A: đọc PayrollEntry EMP001/tháng12 → status=PENDING → bắt đầu tính
 *   Thread B: đọc PayrollEntry EMP001/tháng12 → status=PENDING → bắt đầu tính
 *   Thread A: ghi xong → status=PROCESSED
 *   Thread B: kiểm tra lại → status đã là PROCESSED → throw DuplicatePaymentException
 *
 * Cách phòng tránh (trong PayrollEntryRepository):
 *   - Optimistic Locking: kiểm tra version trước khi ghi
 *   - Synchronized block: chỉ 1 thread được check-then-act tại một thời điểm
 *   - Double-check sau khi acquire lock
 *
 * Cách xử lý khi bắt:
 *   - Log cảnh báo (không phải lỗi nghiêm trọng nếu đã có guard)
 *   - Skip entry đó (đã được xử lý bởi thread khác)
 *   - KHÔNG retry (vì entry đã PROCESSED rồi)
 */
public class DuplicatePaymentException extends RuntimeException {

    /** ID của PayrollEntry bị duplicate */
    private final String entryId;

    /** ID nhân viên liên quan */
    private final String empId;

    /** Tháng/năm của đợt lương bị duplicate */
    private final int month;
    private final int year;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * @param entryId ID của PayrollEntry đã PROCESSED
     * @param empId   ID nhân viên
     * @param month   tháng
     * @param year    năm
     */
    public DuplicatePaymentException(String entryId, String empId, int month, int year) {
        super(String.format(
            "Duplicate payment detected: PayrollEntry '%s' for employee '%s' "
            + "(%d/%d) has already been PROCESSED. "
            + "Possible race condition — another thread already completed this entry.",
            entryId, empId, month, year));
        this.entryId = entryId;
        this.empId   = empId;
        this.month   = month;
        this.year    = year;
    }

    /**
     * Constructor với message tuỳ chỉnh.
     */
    public DuplicatePaymentException(String message, String entryId,
                                     String empId, int month, int year) {
        super(message);
        this.entryId = entryId;
        this.empId   = empId;
        this.month   = month;
        this.year    = year;
    }

    /**
     * Constructor ngắn gọn (chỉ biết entryId).
     */
    public DuplicatePaymentException(String entryId) {
        super("Duplicate payment detected for PayrollEntry: " + entryId);
        this.entryId = entryId;
        this.empId   = null;
        this.month   = -1;
        this.year    = -1;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getEntryId() { return entryId; }
    public String getEmpId()   { return empId;   }
    public int    getMonth()   { return month;   }
    public int    getYear()    { return year;    }
}
