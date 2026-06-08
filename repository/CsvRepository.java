package repository;

import model.BaseEntity;
import exception.CsvParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * CsvRepository<T> – Abstract generic base class cho tầng Repository.
 *
 * Trách nhiệm:
 *  - Đọc/ghi toàn bộ file CSV (loadAll / saveAll)
 *  - Tìm kiếm theo ID và theo field
 *  - Thread-safe I/O dùng ReentrantReadWriteLock
 *    (nền tảng cho PayrollEntryRepository và LeaveBalanceRepository dùng)
 *
 * Kiến trúc:
 *  - Subclass chỉ cần implement createEntity() và getFilePath()
 *  - Concurrency logic nâng cao (Optimistic Lock, Synchronized block)
 *    được xử lý ở subclass — KHÔNG đặt ở Controller/View
 *
 * @param <T> Entity type kế thừa BaseEntity
 */
public abstract class CsvRepository<T extends BaseEntity> {

    // ─── Thread-safe lock: nhiều reader đồng thời, chỉ 1 writer ─────────────
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock  readLock  = lock.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    // ─── Abstract methods – subclass bắt buộc implement ─────────────────────

    /**
     * Trả về đường dẫn tuyệt đối tới file CSV.
     * Ví dụ: "data/payroll_entries.csv"
     */
    protected abstract String getFilePath();

    /**
     * Tạo một instance entity rỗng để gọi fromCsvLine().
     * Ví dụ: return new PayrollEntry();
     */
    protected abstract T createEntity();

    // ─── loadAll() ───────────────────────────────────────────────────────────

    /**
     * Đọc toàn bộ file CSV vào List<T>.
     * Bỏ qua dòng header (dòng đầu tiên) và dòng trống.
     *
     * Thread-safe: dùng ReadLock (nhiều thread có thể đọc đồng thời).
     *
     * @return danh sách entity đã parse, không bao giờ null
     * @throws CsvParseException nếu file không tồn tại hoặc lỗi parse
     */
    public List<T> loadAll() {
        readLock.lock();
        try {
            Path path = Paths.get(getFilePath());

            if (!Files.exists(path)) {
                throw new CsvParseException(
                    "File không tồn tại: " + getFilePath(), null, 0);
            }

            List<T> result = new ArrayList<>();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            // Bỏ qua dòng header (index 0)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    T entity = createEntity();
                    entity.fromCsvLine(line);
                    result.add(entity);
                } catch (Exception e) {
                    // Bọc lỗi parse với số dòng để debug dễ hơn
                    throw new CsvParseException(
                        "Lỗi parse dòng " + (i + 1) + " trong " + getFilePath()
                        + ": " + e.getMessage(),
                        line, i + 1);
                }
            }

            return result;

        } catch (CsvParseException e) {
            throw e; // Re-throw không bọc lại
        } catch (IOException e) {
            throw new CsvParseException(
                "Lỗi đọc file " + getFilePath() + ": " + e.getMessage(), null, 0);
        } finally {
            readLock.unlock();
        }
    }

    // ─── saveAll() ───────────────────────────────────────────────────────────

    /**
     * Ghi đè toàn bộ file CSV với danh sách entity hiện tại.
     * Dòng đầu tiên là header (lấy từ getHeader()).
     *
     * Thread-safe: dùng WriteLock (chỉ 1 thread được ghi tại một thời điểm).
     * Dùng file tạm → rename để đảm bảo atomic write (tránh file bị corrupt).
     *
     * @param entities danh sách entity cần lưu
     * @throws CsvParseException nếu lỗi I/O
     */
    public void saveAll(List<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities list cannot be null.");
        }

        writeLock.lock();
        try {
            Path targetPath = Paths.get(getFilePath());
            Path tempPath   = Paths.get(getFilePath() + ".tmp");

            // Đảm bảo thư mục tồn tại
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            // Ghi vào file tạm trước (atomic write pattern)
            try (BufferedWriter writer = Files.newBufferedWriter(
                    tempPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                // Ghi header
                String header = getHeader();
                if (header != null && !header.isBlank()) {
                    writer.write(header);
                    writer.newLine();
                }

                // Ghi từng entity
                for (T entity : entities) {
                    writer.write(entity.toCsvLine());
                    writer.newLine();
                }
            }

            // Atomic rename: thay thế file gốc bằng file tạm
            Files.move(tempPath, targetPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            throw new CsvParseException(
                "Lỗi ghi file " + getFilePath() + ": " + e.getMessage(), null, 0);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── findById() ──────────────────────────────────────────────────────────

    /**
     * Tìm entity theo ID.
     * Thread-safe: dùng ReadLock.
     *
     * @param id ID cần tìm
     * @return Optional chứa entity nếu tìm thấy, Optional.empty() nếu không
     */
    public Optional<T> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        // loadAll() đã acquire readLock bên trong
        return loadAll().stream()
            .filter(e -> id.trim().equals(e.getId()))
            .findFirst();
    }

    // ─── findByField() ───────────────────────────────────────────────────────

    /**
     * Tìm tất cả entity theo giá trị một field cụ thể.
     *
     * Cách hoạt động: load toàn bộ CSV → tìm theo predicate.
     * Subclass có thể override để tối ưu hơn (index cache, v.v.)
     *
     * Thread-safe: dùng ReadLock (thông qua loadAll()).
     *
     * @param fieldName  tên field (dùng cho logging/debug)
     * @param value      giá trị cần tìm
     * @return danh sách entity khớp, có thể rỗng
     */
    public List<T> findByField(String fieldName, String value) {
        if (value == null) {
            return Collections.emptyList();
        }

        List<T> all = loadAll();
        List<T> result = new ArrayList<>();

        for (T entity : all) {
            if (matchesField(entity, fieldName, value)) {
                result.add(entity);
            }
        }

        return result;
    }

    /**
     * So khớp field theo tên.
     * Subclass PHẢI override method này để hỗ trợ findByField().
     *
     * Mặc định: so sánh entity.getId() với value (chỉ hỗ trợ field "id").
     *
     * @param entity    entity cần kiểm tra
     * @param fieldName tên field cần so khớp
     * @param value     giá trị cần khớp
     * @return true nếu field của entity bằng value
     */
    protected boolean matchesField(T entity, String fieldName, String value) {
        if ("id".equalsIgnoreCase(fieldName)) {
            return value.equals(entity.getId());
        }
        // Subclass override để hỗ trợ các field khác
        throw new UnsupportedOperationException(
            "Field '" + fieldName + "' chưa được hỗ trợ trong "
            + getClass().getSimpleName()
            + ". Override matchesField() để thêm support.");
    }

    // ─── Utility methods cho subclass ────────────────────────────────────────

    /**
     * Tìm theo predicate tuỳ ý – dùng nội bộ trong subclass.
     */
    protected List<T> findByPredicate(Predicate<T> predicate) {
        List<T> all = loadAll(); // đã thread-safe
        List<T> result = new ArrayList<>();
        for (T entity : all) {
            if (predicate.test(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Trả về dòng header CSV.
     * Subclass nên override để cung cấp header đúng cho từng file.
     * Mặc định trả về null (không ghi header).
     */
    protected String getHeader() {
        return null;
    }

    /**
     * Đếm tổng số record trong file (không tính header).
     */
    public int count() {
        return loadAll().size();
    }

    /**
     * Kiểm tra file CSV có tồn tại không.
     */
    public boolean fileExists() {
        return Files.exists(Paths.get(getFilePath()));
    }
}
