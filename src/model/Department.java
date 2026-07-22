package model;

public class Department extends BaseEntity {

    private String name;
    private String managerId;
    private int totalEmployees;
    private String location; // 1. Thêm thuộc tính mới ở đây

    public Department() {
    }

    // 2. Cập nhật Constructor đầy đủ tham số
    public Department(String id, String name, String managerId, int totalEmployees, String location) {
        super(id);
        setName(name);
        setManagerId(managerId);
        setTotalEmployees(totalEmployees);
        setLocation(location);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateRequired(name, "Department name");
        this.name = name.trim();
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        validateRequired(managerId, "Manager ID");
        this.managerId = managerId.trim();
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        validateNonNegative(totalEmployees, "Total employees");
        this.totalEmployees = totalEmployees;
    }

    // 3. Thêm Getter và Setter cho Location kèm hàm validate giống các trường khác
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        validateRequired(location, "Location");
        this.location = location.trim();
    }

    // 4. Cập nhật hàm chuyển đổi đối tượng thành dòng dữ liệu CSV (thêm location ở cuối)
    @Override
    public String toCsvLine() {
        return id + "," + name + "," + managerId + "," + totalEmployees + "," + location;
    }

    // 5. Cập nhật hàm phân tách dòng dữ liệu CSV ngược lại vào đối tượng
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");

        String[] parts = csvLine.split(",");

        // Tăng độ dài kiểm tra từ 4 lên 5 trường dữ liệu
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid Department CSV format. Expected 5 fields but got " + parts.length);
        }

        setId(parts[0]);
        setName(parts[1]);
        setManagerId(parts[2]);
        setLocation(parts[3]); // Đọc dữ liệu vị trí từ mảng kết quả cắt chuỗi
        setTotalEmployees(Integer.parseInt(parts[4]));
    }

    // 6. Cập nhật hàm toString() hỗ trợ in log kiểm tra khi test ứng dụng
    @Override
    public String toString() {
        return "Department{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", managerId='" + managerId + '\'' +
                ", totalEmployees=" + totalEmployees +
                ", location='" + location + '\'' +
                '}';
    }
}
