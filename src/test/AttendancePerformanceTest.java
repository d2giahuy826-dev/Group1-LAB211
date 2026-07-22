package test;

// Import các chú thích chuẩn của JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

// Import các hàm kiểm tra kết quả (Assertions)
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Import các lớp thực thể trong dự án của bạn
import repository.AttendanceRepository;
import model.AttendanceRecord;
import java.util.List;

public class AttendancePerformanceTest {

    private AttendanceRepository repo;

    // Thiết lập môi trường sạch trước khi chạy test ca
    @BeforeEach
    public void setUp() {
        repo = new AttendanceRepository();
    }

    @Test
    @DisplayName("Kiểm thử hiệu năng: Tải dữ liệu chấm công dưới 1 giây")
    public void testGetAllPerformance() {
        // Ghi nhận thời gian bắt đầu hệ thống
        long start = System.nanoTime();

        // Thực thi hàm cần đo hiệu năng
        List<AttendanceRecord> records = repo.getAll();

        // Ghi nhận thời gian kết thúc
        long end = System.nanoTime();

        // Quy đổi ra đơn vị mili-giây (ms)
        double timeMs = (end - start) / 1_000_000.0;

        // In nhật ký log ra Console để tiện theo dõi
        System.out.println("Records loaded: " + (records != null ? records.size() : 0));
        System.out.printf("Execution time: %.3f ms%n", timeMs);

        // 1. Kiểm tra tính hợp lệ: Danh sách trả về không được rỗng (null)
        assertNotNull(records, "LỖI: Danh sách dữ liệu chấm công trả về bị null!");

        // 2. Kiểm tra hiệu năng: Thời gian chạy phải nhỏ hơn 1000ms (1 giây)
        // Nếu đạt yêu cầu -> Hiện tích xanh. Nếu quá hạn -> Hiện dấu X đỏ và báo lỗi.
        assertTrue(timeMs < 1000, "LỖI: Thời gian thực thi vượt quá 1 giây (" + timeMs + " ms)");
    }
}