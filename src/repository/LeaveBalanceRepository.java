package repository;

import model.LeaveBalance;
import java.util.ArrayList;
import java.util.List;

public class LeaveBalanceRepository {

    private List<LeaveBalance> balances = new ArrayList<>();

    public void add(LeaveBalance balance) {
        balances.add(balance);
    }

    public LeaveBalance findByEmployeeId(String employeeId) {
        for (LeaveBalance b : balances) {
            if (b.getEmpId().equals(employeeId)) {
                return b;
            }
        }
        return null;
    }

    public List<LeaveBalance> getAll() {
        return balances;
    }
}