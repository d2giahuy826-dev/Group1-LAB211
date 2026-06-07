package repository;

import model.LeaveRequest;
import model.LeaveStatus;
import model.LeaveType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LeaveRequestRepository – Quản lý các đơn xin nghỉ phép (leave_requests.csv).
 *
 * CSV schema (10 cột):
 *   requestId, empId, leaveType, startDate, endDate,
 *   days, reason, status, approvedBy, createdAt
 *
 * Đặc điểm:
 *  - Không cần Optimistic Lock (đơn nghỉ phép được duyệt tuần tự bởi HR)
 *  - Thread-safe I/O kế thừa từ CsvRepository (ReadWriteLock)
 *  - Hỗ trợ tìm kiếm theo empId, status, leaveType để LeaveBalanceRepository dùng
 *
 * Quy trình nghiệp vụ:
 *   1. HR đọc danh sách PENDING       → findByStatus(PENDING)
 *   2. HR duyệt đơn                   → approve(requestId, approverId)
 *   3. LeaveBalanceRepository trừ phép → deductLeave(empId, type, days)
 */
public class LeaveRequestRepository extends CsvRepository<LeaveRequest> {

    // ─── Đường dẫn file CSV ───────────────────────────────────────────────────
    private static final String FILE_PATH = "data/leave_requests.csv";

    // Header khớp chính xác với DataGenerator.generateLeaveRequests()
    private static final String HEADER =
        "requestId,empId,leaveType,startDate,endDate,days,reason,status,approvedBy,createdAt";

    // ─── Abstract method implementations ─────────────────────────────────────

    @Override
    protected String getFilePath() {
        return FILE_PATH;
    }

    @Override
    protected LeaveRequest createEntity() {
        return new LeaveRequest();
    }

    @Override
    protected String getHeader() {
        return HEADER;
    }

    // ─── matchesField ─────────────────────────────────────────────────────────

    /**
     * Hỗ trợ findByField() cho các field:
     *   "empid" / "empId"   → tìm theo nhân viên
     *   "status"             → tìm theo trạng thái (PENDING / APPROVED / REJECTED)
     *   "leavetype" / "type" → tìm theo loại phép (ANNUAL / SICK)
     *   "approvedby"         → tìm theo người duyệt
     *   "id" / "requestid"   → tìm theo ID đơn
     */
    @Override
    protected boolean matchesField(LeaveRequest req, String fieldName, String value) {
        if (value == null) return false;
        return switch (fieldName.toLowerCase()) {
            case "id", "requestid" -> value.equals(req.getId());
            case "empid"           -> value.equals(req.getEmpId());
            case "status"          -> value.equalsIgnoreCase(req.getStatus().name());
            case "leavetype", "type" -> value.equalsIgnoreCase(req.getType().name());
            case "approvedby"      -> value.equals(req.getApprovedBy());
            default -> throw new UnsupportedOperationException(
                "Field '" + fieldName + "' không được hỗ trợ trong LeaveRequestRepository.");
        };
    }

    // ─── Domain-specific query methods ───────────────────────────────────────

    /**
     * Lấy tất cả đơn nghỉ phép của một nhân viên.
     * Dùng bởi: HR xem lịch sử nghỉ phép của nhân viên.
     *
     * @param empId ID nhân viên
     * @return danh sách đơn, có thể rỗng
     */
    public List<LeaveRequest> findByEmpId(String empId) {
        return loadAll().stream()
            .filter(r -> empId.equals(r.getEmpId()))
            .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đơn theo trạng thái.
     * Hay dùng nhất: findByStatus(LeaveStatus.PENDING) để HR duyệt.
     *
     * @param status trạng thái cần lọc
     * @return danh sách đơn khớp
     */
    public List<LeaveRequest> findByStatus(LeaveStatus status) {
        return loadAll().stream()
            .filter(r -> r.getStatus() == status)
            .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đơn của nhân viên theo trạng thái.
     * Dùng bởi: LeaveBalanceRepository để tính số ngày đã dùng (APPROVED).
     *
     * @param empId  ID nhân viên
     * @param status trạng thái cần lọc
     * @return danh sách đơn khớp cả hai điều kiện
     */
    public List<LeaveRequest> findByEmpIdAndStatus(String empId, LeaveStatus status) {
        return loadAll().stream()
            .filter(r -> empId.equals(r.getEmpId()) && r.getStatus() == status)
            .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đơn của nhân viên theo loại phép và trạng thái.
     * Ví dụ: tìm tất cả đơn ANNUAL + APPROVED của EMP0001 để đối chiếu balance.
     *
     * @param empId     ID nhân viên
     * @param leaveType loại phép (ANNUAL / SICK)
     * @param status    trạng thái
     */
    public List<LeaveRequest> findByEmpIdAndTypeAndStatus(
            String empId, LeaveType leaveType, LeaveStatus status) {
        return loadAll().stream()
            .filter(r -> empId.equals(r.getEmpId())
                      && r.getType()   == leaveType
                      && r.getStatus() == status)
            .collect(Collectors.toList());
    }

    // ─── Write operations ─────────────────────────────────────────────────────

    /**
     * Duyệt một đơn nghỉ phép.
     *
     * Quy trình:
     *   1. Load toàn bộ CSV (ReadLock bên trong loadAll)
     *   2. Tìm đơn theo requestId
     *   3. Gọi request.approve(approverId) → đổi status + ghi approvedBy
     *   4. saveAll() với WriteLock
     *
     * Sau khi gọi method này, caller phải tiếp tục gọi
     * LeaveBalanceRepository.deductLeave() để trừ số ngày phép.
     *
     * @param requestId  ID đơn cần duyệt
     * @param approverId ID người duyệt
     * @throws IllegalArgumentException nếu không tìm thấy requestId
     * @throws IllegalStateException    nếu đơn không ở trạng thái PENDING
     */
    public void approve(String requestId, String approverId) {
        writeLock.lock();
        try {
            List<LeaveRequest> all = loadAll();

            LeaveRequest target = all.stream()
                .filter(r -> requestId.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "LeaveRequest không tìm thấy: " + requestId));

            target.approve(approverId); // ném IllegalStateException nếu không PENDING
            saveAll(all);

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Từ chối một đơn nghỉ phép.
     *
     * @param requestId  ID đơn cần từ chối
     * @param approverId ID người từ chối
     * @throws IllegalArgumentException nếu không tìm thấy requestId
     * @throws IllegalStateException    nếu đơn không ở trạng thái PENDING
     */
    public void reject(String requestId, String approverId) {
        writeLock.lock();
        try {
            List<LeaveRequest> all = loadAll();

            LeaveRequest target = all.stream()
                .filter(r -> requestId.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "LeaveRequest không tìm thấy: " + requestId));

            target.reject(approverId); // ném IllegalStateException nếu không PENDING
            saveAll(all);

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Thêm một đơn nghỉ phép mới vào CSV.
     *
     * @param request đơn nghỉ phép mới (status phải là PENDING)
     */
    public void add(LeaveRequest request) {
        writeLock.lock();
        try {
            List<LeaveRequest> all = loadAll();
            all.add(request);
            saveAll(all);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Aggregation helpers ──────────────────────────────────────────────────

    /**
     * Đếm tổng số ngày đã nghỉ APPROVED của một nhân viên theo loại phép.
     * Dùng để đối chiếu với LeaveBalance.annualUsed / sickUsed.
     *
     * @param empId     ID nhân viên
     * @param leaveType loại phép
     * @return tổng số ngày đã dùng (từ các đơn APPROVED)
     */
    public int sumApprovedDays(String empId, LeaveType leaveType) {
        return loadAll().stream()
            .filter(r -> empId.equals(r.getEmpId())
                      && r.getType()   == leaveType
                      && r.getStatus() == LeaveStatus.APPROVED)
            .mapToInt(LeaveRequest::getDays)
            .sum();
    }

    /**
     * Đếm số đơn PENDING chờ duyệt của toàn hệ thống.
     * Dùng để hiển thị dashboard HR.
     */
    public long countPending() {
        return loadAll().stream()
            .filter(r -> r.getStatus() == LeaveStatus.PENDING)
            .count();
    }
}
