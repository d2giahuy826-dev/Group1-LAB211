package repository;
 
import model.User;
 
import java.util.List;
import java.util.Optional;
 
/**
 * UserRepository – Quản lý tài khoản đăng nhập (users.csv).
 *
 * CSV schema (4 cột):
 *   userId, username, password, role
 */
public class UserRepository extends CsvRepository<User> {
 
    private static final String FILE_PATH = "data/users.csv";
    private static final String HEADER = "userId,username,password,role";
 
    @Override
    protected String getFilePath() {
        return FILE_PATH;
    }
 
    @Override
    protected User createEntity() {
        return new User();
    }
 
    @Override
    protected String getHeader() {
        return HEADER;
    }
 
    @Override
    protected boolean matchesField(User user, String fieldName, String value) {
        if (value == null || fieldName == null) {
            return false;
        }
 
        switch (fieldName.toLowerCase()) {
            case "id":
            case "userid":
                return value.equals(user.getId());
 
            case "username":
                return value.equalsIgnoreCase(user.getUsername());
 
            case "role":
                return value.equalsIgnoreCase(user.getRole().name());
 
            default:
                return false;
        }
    }
 
    // ─── Domain-specific query ────────────────────────────────────────────────
 
    /**
     * Tìm user theo username (không phân biệt hoa/thường).
     */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return loadAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }
 
    /**
     * Xác thực đăng nhập: kiểm tra username + password.
     *
     * @return User nếu đúng, Optional.empty() nếu sai username/password
     */
    public Optional<User> authenticate(String username, String password) {
        return findByUsername(username)
                .filter(u -> u.checkPassword(password));
    }
}