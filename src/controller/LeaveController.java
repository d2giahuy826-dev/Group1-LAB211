package controller;

import model.*;
import repository.*;
import exception.InsufficientLeaveException;

import java.time.LocalDate;
import java.util.List;

public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepo;
    private final LeaveBalanceRepository leaveBalanceRepo;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public LeaveController(LeaveRequestRepository leaveRequestRepo,
                           LeaveBalanceRepository leaveBalanceRepo) {
        this.leaveRequestRepo = leaveRequestRepo;
        this.leaveBalanceRepo = leaveBalanceRepo;
    }

    // ─── 1. Submit Leave ──────────────────────────────────────────────────────
    /**
     * Nhân viên nộp đơn xin nghỉ phép.
     * Status mặc định là PENDING.
     */
    public void submitLeave(String empId, LeaveType type,
                            LocalDate startDate, LocalDate endDate,
                            int days, String reason) {

        // Validate input
        if (empId == null || empId.trim().isEmpty()) {
            throw new IllegalArgumentException("empId không được để trống.");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Số ngày nghỉ phải lớn hơn 0.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }
         // Không cho phép tạo đơn cho ngày đã qua (ngày bắt đầu nghỉ ở quá khứ)
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException(
                "Ngay bat dau nghi (" + startDate + ") da o trong qua khu "
                + "(hom nay la " + today + "). Khong the tao don cho ngay da qua.");
        }

        // Đơn nghỉ phép phải được nộp trước ít nhất 2 ngày so với ngày bắt đầu nghỉ
        long daysInAdvance = java.time.temporal.ChronoUnit.DAYS.between(today, startDate);
        if (daysInAdvance < 2) {
            throw new IllegalArgumentException(
                "Dn nghi phep phai duoc nop truoc ngay bat dau nghi it nhat 2 ngay. "
                + "Ngay bat dau nghi (" + startDate + ") chi cach hom nay ("
                + today + ") " + daysInAdvance + " ngày.");
        }

        // Tạo requestId tự động
        String requestId = generateNextRequestId();

        // Tạo LeaveRequest mới với status PENDING
        LeaveRequest request = new LeaveRequest(
                requestId,
                empId,
                type,
                startDate,
                endDate,
                days,
                reason,
                LeaveStatus.PENDING,
                "",                          // approvedBy rỗng vì chưa duyệt
                LocalDate.now().toString()   // createdAt = hôm nay
        );

        leaveRequestRepo.add(request);
        System.out.println("[OK] Đã nộp đơn nghỉ phép: " + requestId);
    }

    // ─── 2. Approve Leave ─────────────────────────────────────────────────────
    /**
     * HR duyệt đơn nghỉ phép.
     * Sau khi duyệt → trừ ngày phép trong LeaveBalance.
     */
    public void approve(String requestId, String approverId)
            throws InsufficientLeaveException {

        // Bước 1: Tìm đơn nghỉ phép
        LeaveRequest request = leaveRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn nghỉ phép: " + requestId));

        String empId = request.getEmpId();
        synchronized (empId.intern()) {
            // Bước 2: Kiểm tra balance còn đủ không
            LeaveBalance balance = leaveBalanceRepo
                    .findByEmployeeId(empId);

            if (balance == null) {
                throw new IllegalStateException(
                        "Không tìm thấy LeaveBalance của: " + empId);
            }

            if (!balance.hasEnoughLeave(request.getType(), request.getDays())) {
                throw new InsufficientLeaveException(
                        "Không đủ ngày phép. Loại: " + request.getType()
                        + ", Cần: " + request.getDays()
                        + ", Còn: " + (request.getType() == LeaveType.ANNUAL
                                ? balance.getAnnualRemaining()
                                : balance.getSickRemaining()));
            }

            // Bước 3: Duyệt đơn
            leaveRequestRepo.approve(requestId, approverId);

            // Bước 4: Trừ ngày phép
            balance.deductLeave(request.getType(), request.getDays());
            leaveBalanceRepo.update(balance);
        }

        System.out.println("[OK] Đã duyệt đơn: " + requestId
                + " | Trừ " + request.getDays() + " ngày "
                + request.getType());
    }

    // ─── 3. Reject Leave ──────────────────────────────────────────────────────
    /**
     * HR từ chối đơn nghỉ phép.
     * Không trừ ngày phép.
     */
    public void reject(String requestId, String approverId) {

        // Kiểm tra đơn tồn tại không
        leaveRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn nghỉ phép: " + requestId));

        leaveRequestRepo.reject(requestId, approverId);
        System.out.println("[OK] Đã từ chối đơn: " + requestId);
    }

    // ─── 4. View Leave Balance ────────────────────────────────────────────────
    /**
     * Nhân viên xem số dư ngày phép còn lại của mình.
     */
    public LeaveBalance getBalance(String empId) {
        return leaveBalanceRepo.findByEmployeeId(empId);
    }

    // ─── 5. List pending requests (cho HR duyệt) ─────────────────────────────
    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepo.findByStatus(LeaveStatus.PENDING);
    }
    
    private final String ID_PREFIX      = "LR";
    private final int    ID_DIGIT_WIDTH = 6; // LR + 6 chữ số → VD: LR000001
 
    /**
     * Sinh requestId moi theo format: "LR" + so tang dan, luon giu du
     * ID_DIGIT_WIDTH chu so (them so 0 phia truoc neu thieu).
     * VD: LR000001, LR000002, ... LR123456.
     */
    private String generateNextRequestId() {
        int maxNumber = leaveRequestRepo.loadAll().stream()
                .map(LeaveRequest::getId)
                .filter(id -> id != null && id.startsWith(ID_PREFIX))
                .map(id -> id.substring(ID_PREFIX.length()))
                .filter(numPart -> numPart.chars().allMatch(Character::isDigit) && !numPart.isEmpty())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
 
        int nextNumber = maxNumber + 1;
        return ID_PREFIX + String.format("%0" + ID_DIGIT_WIDTH + "d", nextNumber);
    }
}