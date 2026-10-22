/** A staff member who manages buses, schedules and users. */
public class Admin extends User {
    private String department;
    private int accessLevel; // 1 = Operator, 2 = Manager, 3 = Super Admin

    public Admin(String userId, String name, String email, String phone, String password,
                 String department, int accessLevel) {
        super(userId, name, email, phone, password);
        setDepartment(department);
        setAccessLevel(accessLevel);
    }

    public String getDepartment() { return department; }
    public int getAccessLevel()   { return accessLevel; }

    public void setDepartment(String department) {
        if (department == null || department.isBlank())
            throw new IllegalArgumentException("Department cannot be empty.");
        this.department = department.trim();
    }

    public void setAccessLevel(int accessLevel) {
        if (accessLevel < 1 || accessLevel > 3)
            throw new IllegalArgumentException("Access level must be 1, 2 or 3.");
        this.accessLevel = accessLevel;
    }

    @Override // Method overriding
    public String getRoleDetails() {
        String[] levels = {"", "Operator", "Manager", "Super Admin"};
        return "ROLE: Administrator (" + levels[accessLevel] + ")\n"
             + "ID: " + getUserId() + "\n"
             + "Name: " + getName() + "  |  Dept: " + department + "\n\n"
             + "Privileges:\n"
             + "  - Add / update / remove buses and schedules\n"
             + "  - View and manage all registered users\n"
             + "  - View all bookings and payment reports";
    }
}
