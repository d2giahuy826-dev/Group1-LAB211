package test;

// 1. Import các thư viện kiểm thử chuẩn của JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

// 2. Import các lớp thực thể trong dự án của bạn
import model.Employee;
import model.EmpType;
import model.EmployeeStatus;
import repository.EmployeeRepository;

import java.time.LocalDate;

public class EmployeeRepositoryTest {

    private EmployeeRepository repo;

    @BeforeEach
    public void setUp() {
        // Khởi tạo repository sạch trước khi chạy bài test
        repo = new EmployeeRepository();
    }

    @Test
    @DisplayName("Kiểm thử chu trình CRUD tự động hoàn chỉnh của Nhân Viên")
    public void testCompleteEmployeeCRUD() {
        // Tạo một ID duy nhất bằng thời gian hệ thống để không bao giờ bị trùng với dữ liệu cũ
        String testEmpId = "EMP_TEST_" + System.currentTimeMillis(); 
        
        // Lấy số lượng nhân viên ban đầu trước khi thêm mới
        int initialCount = repo.count();

        // ==========================================
        // 1. KIỂM THỬ THAO TÁC: CREATE (THÊM MỚI)
        // ==========================================
        Employee newEmp = new Employee(
                testEmpId,
                "Nguyen Van Test",
                "DEPT01",
                EmpType.FULLTIME,
                15000000.0,
                0.1,
                LocalDate.now(),
                EmployeeStatus.ACTIVE
        );
        
        // Thực hiện thêm nhân viên vào file CSV thông qua repository
        repo.add(newEmp);

        // Đọc lại để kiểm tra (Sửa lỗi Optional: Dùng .orElse(null) để lấy Employee ra an toàn)
        Employee foundEmp = repo.findById(testEmpId).orElse(null);
        
        // Kiểm tra xem đối tượng tìm thấy có bị null không
        assertNotNull(foundEmp, "LỖI [CREATE]: Không tìm thấy nhân viên vừa thêm trong file CSV!");
        assertEquals("Nguyen Van Test", foundEmp.getFullName(), "LỖI [CREATE]: Tên nhân viên lưu vào bị sai dữ liệu gốc!");

        // ==========================================
        // 2. KIỂM THỬ THAO TÁC: COUNT (ĐẾM BẢN GHI)
        // ==========================================
        int countAfterAdd = repo.count();
        assertEquals(initialCount + 1, countAfterAdd, "LỖI [COUNT]: Số lượng bản ghi trong file không tăng lên sau khi thêm!");

        // ==========================================
        // 3. KIỂM THỬ THAO TÁC: UPDATE (CẬP NHẬT)
        // ==========================================
        String updatedName = "Nguyen Van Test Hoan Toan Moi";
        foundEmp.setFullName(updatedName);
        
        // Gọi hàm update xuống file CSV
        boolean isUpdateSuccess = repo.update(foundEmp);
        assertTrue(isUpdateSuccess, "LỖI [UPDATE]: Hàm repo.update(emp) trả về giá trị false!");

        // Đọc lại lần nữa và gỡ Optional ra một cách an toàn
        Employee empAfterUpdate = repo.findById(testEmpId).orElse(null);
        assertNotNull(empAfterUpdate, "LỖI [UPDATE]: Không tìm thấy nhân viên sau khi cập nhật!");
        assertEquals(updatedName, empAfterUpdate.getFullName(), "LỖI [UPDATE]: Tên nhân viên trong file CSV chưa thực sự thay đổi!");

        // ==========================================
        // 4. KIỂM THỬ THAO TÁC: DELETE (XÓA)
        // ==========================================
        // Thực hiện lệnh xóa nhân viên vừa tạo khỏi file CSV dựa vào ID
        boolean isDeleteSuccess = repo.delete(testEmpId);
        assertTrue(isDeleteSuccess, "LỖI [DELETE]: Hàm repo.delete(id) trả về giá trị false!");

        // Đọc lại lần cuối: Nếu xóa đúng thì hàm findById bắt buộc phải trả về Optional rỗng (tương ứng với null khi orElse)
        Employee empAfterDelete = repo.findById(testEmpId).orElse(null);
        assertNull(empAfterDelete, "LỖI [DELETE]: Nhân viên vẫn tồn tại trong file CSV sau khi thực hiện lệnh xóa!");

        // Kiểm tra số lượng bản ghi quay về đúng số lượng ban đầu
        int finalCount = repo.count();
        assertEquals(initialCount, finalCount, "LỖI [DELETE]: Số lượng bản ghi trong file chưa giảm đi sau khi xóa!");
    }
}

