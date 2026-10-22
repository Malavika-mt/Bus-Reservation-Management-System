/** A customer who searches buses and books tickets. */
public class Passenger extends User {
    private int age;
    private int loyaltyPoints;

    public Passenger(String userId, String name, String email, String phone, String password, int age) {
        super(userId, name, email, phone, password);
        setAge(age);
        this.loyaltyPoints = 0;
    }

    public int getAge() { return age; }
    public int getLoyaltyPoints() { return loyaltyPoints; }

    public void setAge(int age) {
        if (age < 5 || age > 120) throw new IllegalArgumentException("Age must be between 5 and 120.");
        this.age = age;
    }

    public void addLoyaltyPoints(int points) {
        if (points <= 0) throw new IllegalArgumentException("Points must be positive.");
        loyaltyPoints += points;
    }

    @Override // Method overriding
    public String getRoleDetails() {
        return "ROLE: Passenger\n"
             + "ID: " + getUserId() + "\n"
             + "Name: " + getName() + "  (Age " + age + ")\n"
             + "Loyalty points: " + loyaltyPoints + "\n\n"
             + "Access:\n"
             + "  - Search buses by source / destination / date\n"
             + "  - Book and cancel own tickets\n"
             + "  - Make payments and view own booking history";
    }
}
