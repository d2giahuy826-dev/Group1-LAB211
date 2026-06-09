package model;

/**
 * BaseEntity – Lớp trừu tượng gốc cho tất cả entity trong hệ thống.
 *
 * Cung cấp:
 *  - id chung
 *  - implement CsvMappable (toCsvLine / fromCsvLine)
 *  - các phương thức validate tái sử dụng
 */
public abstract class BaseEntity implements CsvMappable {

    protected String id;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public BaseEntity() {}

    public BaseEntity(String id) {
        setId(id);
    }

    // ─── Getter / Setter ──────────────────────────────────────────────────────

    public String getId() { return id; }

    public void setId(String id) {
        validateRequired(id, "ID");
        this.id = id.trim();
    }

    // ─── Validate helpers (dùng lại trong subclass) ───────────────────────────

    /**
     * Ném IllegalArgumentException nếu value null hoặc rỗng.
     */
    protected void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be blank.");
        }
    }

    /**
     * Ném IllegalArgumentException nếu value âm (double).
     */
    protected void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative. Got: " + value);
        }
    }

    /**
     * Ném IllegalArgumentException nếu value âm (int).
     */
    protected void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative. Got: " + value);
        }
    }

    // ─── CsvMappable (subclass bắt buộc override) ────────────────────────────

    @Override
    public abstract String toCsvLine();

    @Override
    public abstract void fromCsvLine(String csvLine);

    // ─── toString ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "'}";
    }
}
