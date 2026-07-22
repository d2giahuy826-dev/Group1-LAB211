package controller;
 
import model.User;
import repository.UserRepository;
 
import java.util.Optional;
 
/**
 * AuthController – Điều phối đăng nhập / đăng xuất.
 *
 * Lưu trạng thái user đang đăng nhập (session đơn giản trong bộ nhớ).
 */
public class AuthController {
 
    private final UserRepository userRepo;
    private User currentUser; // user đang đăng nhập, null nếu chưa login
 
    public AuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }
 
    /**
     * Đăng nhập với username + password.
     *
     * @return true nếu đăng nhập thành công
     */
    public boolean login(String username, String password) {
        Optional<User> result = userRepo.authenticate(username, password);
 
        if (result.isPresent()) {
            this.currentUser = result.get();
            return true;
        }
 
        return false;
    }
 
    /**
     * Đăng xuất — xoá session hiện tại.
     */
    public void logout() {
        this.currentUser = null;
    }
 
    /**
     * Kiểm tra đã đăng nhập chưa.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
 
    /**
     * Lấy user đang đăng nhập.
     */
    public User getCurrentUser() {
        return currentUser;
    }
}