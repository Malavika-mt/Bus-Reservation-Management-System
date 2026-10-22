import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Abstract base class for every user of the Bus Reservation System. */
public abstract class User {
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;

    protected User(String userId, String name, String email, String phone, String password) {
        setUserId(userId);
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPassword(password);
    }

    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }
    public String getPhone()  { return phone; }
    public String getRole()   { return getClass().getSimpleName(); }

    public void setUserId(String userId) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("User ID cannot be empty.");
        this.userId = userId.trim();
    }

    public void setName(String name) {
        if (name == null || !name.trim().matches("[A-Za-z][A-Za-z .]{1,49}"))
            throw new IllegalArgumentException("Name must be 2-50 letters (spaces and dots allowed).");
        this.name = name.trim();
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        this.email = email.trim().toLowerCase();
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("[6-9]\\d{9}"))
            throw new IllegalArgumentException("Phone must be a 10-digit mobile number.");
        this.phone = phone.trim();
    }

    public void setPassword(String password) {
        if (password == null || password.length() < 6
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            throw new IllegalArgumentException("Password must be 6+ characters with a letter and a digit.");
        this.passwordHash = hash(password);
    }

    /** Returns the stored password hash. */
    public String getPasswordHash() { return passwordHash; }

    /** Restores a hash loaded from the database. */
    void restorePasswordHash(String storedHash) {
        if (storedHash == null || storedHash.length() != 64)
            throw new IllegalArgumentException("Invalid stored password hash.");
        this.passwordHash = storedHash;
    }

    public boolean authenticate(String password) {
        return password != null && passwordHash.equals(hash(password));
    }

    private static String hash(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                int value = b & 0xff;
                sb.append(HEX_DIGITS[value >>> 4]).append(HEX_DIGITS[value & 0x0f]);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Hashing unavailable", e);
        }
    }

    public abstract String getRoleDetails();

    @Override
    public String toString() {
        return getRole() + "[" + userId + ", " + name + ", " + email + "]";
    }
}
