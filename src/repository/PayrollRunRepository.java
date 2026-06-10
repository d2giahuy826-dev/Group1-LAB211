package repository;

import model.PayrollRun;
import model.RunStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PayrollRunRepository – Quản lý các đợt chạy lương (payroll_runs.csv).
 *
 * CSV schema (9 cột):
 *   runId, month, year, triggeredBy,
 *   totalEmployees, totalNetPay, status, startedAt, completedAt
 *
 * Đặc điểm:
 *  - File nhỏ (chỉ 12 dòng/năm) → không cần cache phức tạp
 *  - Không cần Optimistic Lock (PayrollRun được update tuần tự, không concurrent)
 *  - Thread-safe I/O kế thừa từ CsvRepository (ReadWriteLock)
 *  - Simulator (Week 8) dùng findPending() để lấy run cần xử lý
 */
public class PayrollRunRepository extends CsvRepository<PayrollRun> {

    // ─── Đường dẫn file CSV ───────────────────────────────────────────────────
    private static final String FILE_PATH = "data/payroll_runs.csv";

    // Header khớp chính xác với DataGenerator.generatePayrollRuns()
    private static final String HEADER =
        "runId,month,year,triggeredBy,totalEmployees,totalNetPay,status,startedAt,completedAt";

    // ─── Abstract method implementations ─────────────────────────────────────

    @Override
    protected String getFilePath() {
        return FILE_PATH;
    }

    @Override
    protected PayrollRun createEntity() {
        return new PayrollRun();
    }

    @Override
    protected String getHeader() {
        return HEADER;
    }

    // ─── matchesField: hỗ trợ tìm theo các field của PayrollRun ─────────────

    /**
     * Hỗ trợ findByField() cho các field:
     *   "status"        → so khớp RunStatus (PENDING / COMPLETED)
     *   "month"         → so khớp tháng (String "1" .. "12")
     *   "year"          → so khớp năm
     *   "triggeredBy"   → so khớp người/system kích hoạt
     *   "id" / "runId"  → so khớp ID (kế thừa từ base)
     */
    @Override
protected boolean matchesField(PayrollRun run, String fieldName, String value) {
    if (value == null || fieldName == null) {
        return false;
    }

    switch (fieldName.toLowerCase()) {
        case "id":
        case "runid":
            return value.equals(run.getId());

        case "status":
            return value.equalsIgnoreCase(run.getStatus().name());

        case "month":
            return value.equals(String.valueOf(run.getMonth()));

        case "year":
            return value.equals(String.valueOf(run.getYear()));

        case "triggeredby":
            return value.equalsIgnoreCase(run.getTriggeredBy());

        default:
            throw new UnsupportedOperationException(
                "Field '" + fieldName + "' is not supported in PayrollRunRepository."
            );
    }
}

    // ─── Domain-specific query methods ───────────────────────────────────────

    /**
     * Tìm đợt chạy lương theo tháng và năm.
     * Trả về Optional.empty() nếu không tìm thấy.
     *
     * Dùng bởi: Simulator để kiểm tra run tháng hiện tại đã tồn tại chưa.
     *
     * @param month tháng (1–12)
     * @param year  năm
     */
    public Optional<PayrollRun> findByMonthAndYear(int month, int year) {
        return loadAll().stream()
            .filter(r -> r.getMonth() == month && r.getYear() == year)
            .findFirst();
    }

    /**
     * Lấy tất cả đợt chạy lương ở trạng thái PENDING.
     * Simulator (Week 8) gọi method này để biết run nào cần xử lý.
     *
     * @return danh sách PayrollRun đang PENDING, có thể rỗng
     */
    public List<PayrollRun> findPending() {
        return loadAll().stream()
            .filter(r -> r.getStatus() == RunStatus.PENDING)
            .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đợt chạy lương đã COMPLETED.
     *
     * @return danh sách PayrollRun đã hoàn thành
     */
    public List<PayrollRun> findCompleted() {
        return loadAll().stream()
            .filter(r -> r.getStatus() == RunStatus.COMPLETED)
            .collect(Collectors.toList());
    }

    /**
     * Lấy đợt chạy lương mới nhất (theo month/year cao nhất).
     * Dùng để hiển thị dashboard hoặc kiểm tra trạng thái hệ thống.
     *
     * @return Optional chứa PayrollRun mới nhất
     */
    public Optional<PayrollRun> findLatest() {
        return loadAll().stream()
            .max((a, b) -> {
                int yearCmp = Integer.compare(a.getYear(), b.getYear());
                return yearCmp != 0 ? yearCmp : Integer.compare(a.getMonth(), b.getMonth());
            });
    }

    /**
     * Đánh dấu một PayrollRun là COMPLETED và lưu lại.
     * Được gọi sau khi Simulator xử lý xong toàn bộ PayrollEntry của tháng đó.
     *
     * Thread-safe: WriteLock từ saveAll() trong CsvRepository.
     *
     * @param runId         ID của run cần đánh dấu hoàn thành
     * @param completedDate ngày hoàn thành (format: "yyyy-MM-dd")
     * @throws IllegalArgumentException nếu không tìm thấy runId
     */
    public void markCompleted(String runId, String completedDate) {
        writeLock.lock();
        try {
            List<PayrollRun> all = loadAll();

            boolean found = false;
            for (PayrollRun run : all) {
                if (runId.equals(run.getId())) {
                    run.markCompleted(completedDate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalArgumentException(
                    "PayrollRun không tìm thấy với ID: " + runId);
            }

            saveAll(all);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Thêm một PayrollRun mới vào CSV.
     * Kiểm tra trùng tháng/năm trước khi thêm.
     *
     * @param run PayrollRun cần thêm
     * @throws IllegalStateException nếu đã tồn tại run cho tháng/năm đó
     */
    public void add(PayrollRun run) {
        writeLock.lock();
        try {
            List<PayrollRun> all = loadAll();

            // Kiểm tra trùng tháng/năm
            boolean exists = all.stream()
                .anyMatch(r -> r.getMonth() == run.getMonth()
                            && r.getYear()  == run.getYear());
            if (exists) {
                throw new IllegalStateException(
                    "Đã tồn tại PayrollRun cho tháng " + run.getMonth()
                    + "/" + run.getYear() + ". Không thể thêm trùng.");
            }

            all.add(run);
            saveAll(all);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Tính tổng lương thực nhận của tất cả các run đã COMPLETED.
     * Dùng để hiển thị báo cáo tổng kết năm.
     *
     * @return tổng netPay tất cả run COMPLETED
     */
    public long sumTotalNetPayCompleted() {
        return loadAll().stream()
            .filter(r -> r.getStatus() == RunStatus.COMPLETED)
            .mapToLong(PayrollRun::getTotalNetPay)
            .sum();
    }
}
