package controller;

import model.AttendanceRecord;
import repository.AttendanceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AttendanceController – Điều phối chấm công & yêu cầu điều chỉnh công.
 *
 * LƯU Ý QUAN TRỌNG:
 * Model AttendanceRecord hiện lưu dữ liệu TỔNG HỢP THEO THÁNG
 * (workDays, absenceDays, overtimeHours, lateCount) — không lưu từng lượt
 * check-in/check-out theo giờ như thực tế. Vì vậy:
 *   - checkIn()  → cộng thêm 1 "workDay" vào bản ghi tháng hiện tại (tạo mới nếu chưa có)
 *   - checkOut() → chỉ xác nhận, không có dữ liệu chi tiết theo giờ để lưu thêm
 *
 * Yêu cầu điều chỉnh công (Attendance Adjustment Request) chưa có CSV riêng
 * trong project gốc, nên được lưu TẠM trong bộ nhớ (in-memory) của controller
 * này. Khi HR duyệt → mới ghi đè xuống attendance.csv thật sự. Nếu cần lưu
 * bền vững (persist) thì nên tách ra một AttendanceAdjustmentRepository +
 * CSV riêng.
 */
public class AttendanceController {

    private final AttendanceRepository attendanceRepo = new AttendanceRepository();

    private final List<AdjustmentRequest> pendingRequests = new ArrayList<>();
    private int requestCounter = 1;

    // ─── Check in / Check out ────────────────────────────────────────────────

    public AttendanceRecord checkIn(String empId) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = findOrCreate(empId, today.getMonthValue(), today.getYear());
        record.setWorkDays(record.getWorkDays() + 1);
        attendanceRepo.saveAll(replaceOrAdd(record));
        return record;
    }

    public AttendanceRecord checkOut(String empId) {
        LocalDate today = LocalDate.now();
        return findOrCreate(empId, today.getMonthValue(), today.getYear());
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    public List<AttendanceRecord> findByEmployeeId(String empId) {
        return attendanceRepo.findByEmployeeId(empId);
    }

    public List<AttendanceRecord> getAll() {
        return attendanceRepo.getAll();
    }

    /** Tổng hợp workDays / absenceDays / overtimeHours toàn công ty theo tháng. */
    public String summarize(int month, int year) {
        List<AttendanceRecord> list = attendanceRepo.getAll().stream()
                .filter(r -> r.getMonth() == month && r.getYear() == year)
                .collect(Collectors.toList());

        int totalWork    = list.stream().mapToInt(AttendanceRecord::getWorkDays).sum();
        int totalAbsence = list.stream().mapToInt(AttendanceRecord::getAbsenceDays).sum();
        int totalOT      = list.stream().mapToInt(AttendanceRecord::getOvertimeHours).sum();

        return String.format(
            "Thang %d/%d - %d nhan vien co du lieu | Tong ngay cong: %d | Tong ngay vang: %d | Tong gio OT: %d",
            month, year, list.size(), totalWork, totalAbsence, totalOT);
    }

    // ─── Adjustment request workflow (in-memory) ─────────────────────────────

    public String submitAdjustmentRequest(String empId, int month, int year,
                                          int newWorkDays, int newAbsenceDays,
                                          int newOvertimeHours, String reason) {
        String requestId = "ADJ_" + (requestCounter++);
        pendingRequests.add(new AdjustmentRequest(
                requestId, empId, month, year,
                newWorkDays, newAbsenceDays, newOvertimeHours, reason));
        return requestId;
    }

    public List<AdjustmentRequest> getPendingRequests() {
        return pendingRequests;
    }

    public boolean approveAdjustment(String requestId, String approverId) {
        Optional<AdjustmentRequest> opt = findPending(requestId);
        if (!opt.isPresent()) return false;
        AdjustmentRequest req = opt.get();

        AttendanceRecord record = findOrCreate(req.empId, req.month, req.year);
        record.setWorkDays(req.newWorkDays);
        record.setAbsenceDays(req.newAbsenceDays);
        record.setOvertimeHours(req.newOvertimeHours);
        attendanceRepo.saveAll(replaceOrAdd(record));

        pendingRequests.remove(req);
        return true;
    }

    public boolean rejectAdjustment(String requestId, String approverId) {
        Optional<AdjustmentRequest> opt = findPending(requestId);
        if (!opt.isPresent()) return false;
        pendingRequests.remove(opt.get());
        return true;
    }

    private Optional<AdjustmentRequest> findPending(String requestId) {
        return pendingRequests.stream()
                .filter(r -> r.requestId.equals(requestId))
                .findFirst();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AttendanceRecord findOrCreate(String empId, int month, int year) {
        return attendanceRepo.getAll().stream()
                .filter(r -> r.getEmpId().equals(empId) && r.getMonth() == month && r.getYear() == year)
                .findFirst()
                .orElseGet(() -> new AttendanceRecord(
                        "ATT_" + empId + "_" + month + "_" + year,
                        empId, month, year, 0, 0, 0, 0));
    }

    private List<AttendanceRecord> replaceOrAdd(AttendanceRecord record) {
        List<AttendanceRecord> all = attendanceRepo.getAll();
        all.removeIf(r -> r.getId().equals(record.getId()));
        all.add(record);
        return all;
    }

    // ─── DTO ─────────────────────────────────────────────────────────────────

    public static class AdjustmentRequest {
        public final String requestId;
        public final String empId;
        public final int month;
        public final int year;
        public final int newWorkDays;
        public final int newAbsenceDays;
        public final int newOvertimeHours;
        public final String reason;

        public AdjustmentRequest(String requestId, String empId, int month, int year,
                                  int newWorkDays, int newAbsenceDays, int newOvertimeHours,
                                  String reason) {
            this.requestId = requestId;
            this.empId = empId;
            this.month = month;
            this.year = year;
            this.newWorkDays = newWorkDays;
            this.newAbsenceDays = newAbsenceDays;
            this.newOvertimeHours = newOvertimeHours;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format(
                "[%s] empId=%s %d/%d -> workDays=%d, absenceDays=%d, OT=%d | Ly do: %s",
                requestId, empId, month, year, newWorkDays, newAbsenceDays, newOvertimeHours, reason);
        }
    }
}
