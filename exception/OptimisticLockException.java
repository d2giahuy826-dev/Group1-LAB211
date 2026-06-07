package exception;

/**
 * OptimisticLockException – Ném khi phát hiện xung đột version trong Optimistic Locking.
 *
 * Kịch bản xảy ra:
 *   1. Thread A đọc PayrollEntry (version = 0)
 *   2. Thread B cũng đọc PayrollEntry đó (version = 0)
 *   3. Thread B cập nhật thành công → version trở thành 1
 *   4. Thread A cố ghi lại với version = 0 → CONFLICT → throw OptimisticLockException
 *
 * Cách xử lý đề xuất:
 *   - Retry lại toàn bộ thao tác (read → compute → write)
 *   - Giới hạn số lần retry (ví dụ: MAX_RETRY = 3)
 *   - Nếu vẫn thất bại → báo lỗi cho PayrollSimulator
 *
 * Quan hệ:
 *   - PayrollEntryRepository.processPayroll() ném exception này
 *   - LeaveBalanceRepository.deductLeave()    ném exception này
 *   - PayrollSimulator (Week 8) phải bắt và retry
 */
public class OptimisticLockException extends RuntimeException {

    /** ID của entity bị conflict */
    private final String entityId;

    /** Version đang giữ trong memory (stale) */
    private final int expectedVersion;

    /** Version thực tế đang có trong file CSV */
    private final int actualVersion;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * @param entityId        ID của entity bị conflict (ví dụ: "PE00000001")
     * @param expectedVersion version thread đang giữ (đọc từ trước)
     * @param actualVersion   version thực tế hiện tại trong CSV
     */
    public OptimisticLockException(String entityId, int expectedVersion, int actualVersion) {
        super(String.format(
            "Optimistic lock conflict on entity '%s': expected version=%d but found version=%d. "
            + "Another thread has already modified this record. Please retry.",
            entityId, expectedVersion, actualVersion));
        this.entityId        = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion   = actualVersion;
    }

    /**
     * Constructor với message tuỳ chỉnh (cho các trường hợp đặc biệt).
     */
    public OptimisticLockException(String message, String entityId,
                                   int expectedVersion, int actualVersion) {
        super(message);
        this.entityId        = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion   = actualVersion;
    }

    /**
     * Constructor với cause (nếu exception bọc từ exception khác).
     */
    public OptimisticLockException(String entityId, int expectedVersion,
                                   int actualVersion, Throwable cause) {
        super(String.format(
            "Optimistic lock conflict on entity '%s': expected=%d, actual=%d",
            entityId, expectedVersion, actualVersion), cause);
        this.entityId        = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion   = actualVersion;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /** ID entity bị conflict */
    public String getEntityId() { return entityId; }

    /** Version thread đang giữ (stale) */
    public int getExpectedVersion() { return expectedVersion; }

    /** Version thực tế trong CSV */
    public int getActualVersion() { return actualVersion; }

    /**
     * Trả về true nếu đây là conflict do thread khác đã ghi thành công.
     * (actualVersion > expectedVersion)
     */
    public boolean isConcurrentModification() {
        return actualVersion > expectedVersion;
    }
}
