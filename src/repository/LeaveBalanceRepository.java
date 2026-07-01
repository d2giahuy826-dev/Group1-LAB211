package repository;

import model.LeaveBalance;
import model.LeaveType;
import java.util.List;
import java.util.Optional;

public class LeaveBalanceRepository extends CsvRepository<LeaveBalance> {

    @Override
    protected String getFilePath() {
        return "data/leave_balance.csv";
    }

    @Override
    protected LeaveBalance createEntity() {
        return new LeaveBalance();
    }

    @Override
    protected String getHeader() {
        return "balanceId,empId,year,annualTotal,annualUsed,annualRemaining,sickTotal,sickUsed,sickRemaining,version";
    }

    @Override
    protected boolean matchesField(LeaveBalance lb, String fieldName, String value) {
        if ("empId".equalsIgnoreCase(fieldName)) {
            return value.equals(lb.getEmpId());
        }
        return super.matchesField(lb, fieldName, value);
    }

    // --- WRAPPER FIX (Sửa lỗi cho Test) ---
    
    public List<LeaveBalance> getAll() {
        return loadAll();
    }

    // Đã sửa để trả về LeaveBalance thay vì Optional, giúp khớp với file Test
    public LeaveBalance findByEmployeeId(String empId) {
        if (empId == null || empId.trim().isEmpty()) {
            return null;
        }
        List<LeaveBalance> matches = loadAll().stream()
                .filter(lb -> empId.trim().equals(lb.getEmpId()))
                .collect(java.util.stream.Collectors.toList());

        if (matches.isEmpty()) {
            return null;
        }

        int currentYear = java.time.LocalDate.now().getYear();
        return matches.stream()
                .filter(lb -> lb.getYear() == currentYear)
                .findFirst()
                .orElse(matches.get(0));
    }

    // --- CÁC HÀM NGHIỆP VỤ ---

    public Optional<LeaveBalance> findByEmpIdAndYear(String empId, int year) {
        return loadAll().stream()
                .filter(lb -> lb.getEmpId().equals(empId) && lb.getYear() == year)
                .findFirst();
    }

    public boolean deductWithOptimisticLock(String empId, int year, LeaveType type, int days, int expectedVersion) {
        writeLock.lock();
        try {
            List<LeaveBalance> list = loadAll();
            for (LeaveBalance lb : list) {
                if (lb.getEmpId().equals(empId) && lb.getYear() == year) {
                    
                    // Kiểm tra version (Optimistic Locking)
                    if (lb.getVersion() == expectedVersion) {
                        // Kiểm tra số dư và trừ ngày nghỉ
                        if (lb.hasEnoughLeave(type, days)) {
                            lb.deductLeave(type, days);
                            lb.setVersion(lb.getVersion() + 1); 
                            
                            saveAll(list);
                            return true;
                        }
                    }
                }
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }
}

