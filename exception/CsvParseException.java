package exception;

/**
 * CsvParseException – Ném khi không thể parse dòng CSV thành entity.
 *
 * Xảy ra trong CsvRepository.loadAll() khi:
 *   - Số cột không đúng (ví dụ: PayrollEntry cần 14 cột mà chỉ có 13)
 *   - Kiểu dữ liệu sai (parseInt() thất bại, LocalDate.parse() thất bại)
 *   - File CSV không tồn tại hoặc bị corrupt
 *   - Dòng CSV có ký tự đặc biệt không được escape đúng
 *
 * Thông tin đính kèm:
 *   - rawLine: nội dung dòng CSV gây lỗi (để debug)
 *   - lineNumber: số dòng trong file (1-indexed, tính cả header)
 *
 * Đây là RuntimeException vì lỗi CSV thường không recover được trong runtime —
 * cần sửa file CSV hoặc DataGenerator trước khi chạy lại.
 */
public class CsvParseException extends RuntimeException {

    /** Nội dung dòng CSV gây lỗi (null nếu lỗi I/O tổng quát) */
    private final String rawLine;

    /** Số dòng trong file (1-indexed, tính cả header). 0 = không xác định */
    private final int lineNumber;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * @param message    mô tả lỗi
     * @param rawLine    nội dung dòng CSV gây lỗi (có thể null)
     * @param lineNumber số dòng trong file (0 nếu không xác định)
     */
    public CsvParseException(String message, String rawLine, int lineNumber) {
        super(buildMessage(message, rawLine, lineNumber));
        this.rawLine    = rawLine;
        this.lineNumber = lineNumber;
    }

    /**
     * Constructor với cause (bọc exception gốc như NumberFormatException).
     */
    public CsvParseException(String message, String rawLine,
                             int lineNumber, Throwable cause) {
        super(buildMessage(message, rawLine, lineNumber), cause);
        this.rawLine    = rawLine;
        this.lineNumber = lineNumber;
    }

    /**
     * Constructor ngắn gọn (chỉ message, không có rawLine).
     */
    public CsvParseException(String message) {
        super(message);
        this.rawLine    = null;
        this.lineNumber = 0;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static String buildMessage(String message, String rawLine, int lineNumber) {
        StringBuilder sb = new StringBuilder(message);
        if (lineNumber > 0) {
            sb.append(" [Line ").append(lineNumber).append("]");
        }
        if (rawLine != null && !rawLine.isBlank()) {
            // Cắt ngắn nếu quá dài
            String preview = rawLine.length() > 100
                ? rawLine.substring(0, 100) + "..."
                : rawLine;
            sb.append(" | Raw: \"").append(preview).append("\"");
        }
        return sb.toString();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /**
     * Nội dung dòng CSV gây lỗi (dùng để log / hiển thị).
     * Có thể null nếu lỗi I/O tổng quát (file không tồn tại, v.v.)
     */
    public String getRawLine() { return rawLine; }

    /**
     * Số dòng trong file (1-indexed, tính cả header).
     * 0 nếu không xác định (lỗi I/O tổng quát).
     */
    public int getLineNumber() { return lineNumber; }

    /**
     * Kiểm tra có thông tin dòng lỗi không.
     */
    public boolean hasLineInfo() { return lineNumber > 0 && rawLine != null; }
}
