package model;
 
/**
 * User – Tài khoản đăng nhập hệ thống.
 *
 * CSV columns (4):
 *   userId, username, password, role
 *
 * role: EMPLOYEE / HR_STAFF / PAYROLL_STAFF
 */
public class User extends BaseEntity {
 
    private String username;
    private String password;
    private UserRole role;
 
    // ─── Constructors ─────────────────────────────────────────────────────────
 
    public User() {}
 
    public User(String userId, String username, String password, UserRole role) {
        super(userId);
        setUsername(username);
        setPassword(password);
        setRole(role);
    }
 
    // ─── CsvMappable ─────────────────────────────────────────────────────────
 
    @Override
    public String toCsvLine() {
        return String.join(",", id, username, password, role.name());
    }
 
    @Override
    public void fromCsvLine(String csvLine) {
        validateRequired(csvLine, "CSV line");
 
        String[] parts = csvLine.split(",", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid User CSV: expected 4 columns, got " + parts.length
                    + " → [" + csvLine + "]");
        }
 
        setId(parts[0].trim());
        setUsername(parts[1].trim());
        setPassword(parts[2].trim());
        setRole(UserRole.fromString(parts[3].trim()));
    }
 
    // ─── Business Logic ───────────────────────────────────────────────────────
 
    /**
     * Kiểm tra mật khẩu có khớp không.
     */
    public boolean checkPassword(String inputPassword) {
        return password != null && password.equals(inputPassword);
    }
 
    // ─── Getters / Setters ────────────────────────────────────────────────────
 
    public String getUsername() { return username; }
    public void setUsername(String username) {
        validateRequired(username, "Username");
        this.username = username.trim();
    }
 
    public String getPassword() { return password; }
    public void setPassword(String password) {
        validateRequired(password, "Password");
        this.password = password;
    }
 
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) {
        if (role == null) throw new IllegalArgumentException("Role is required.");
        this.role = role;
    }
 
    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username
               + "', role=" + role + "}";
    }
}