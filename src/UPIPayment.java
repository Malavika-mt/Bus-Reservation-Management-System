/** UPI payment with ID validation. */
public class UPIPayment extends Payment {
    private final String upiId;

    public UPIPayment(String upiId) {
        this.upiId = upiId == null ? "" : upiId.trim();
    }

    @Override public String getMode() { return "UPI"; }

    @Override // Method overriding
    public Receipt processPayment(double amount) throws PaymentFailedException {
        validateAmount(amount);
        if (!upiId.matches("[A-Za-z0-9._-]{2,50}@[A-Za-z]{2,20}"))
            throw new PaymentFailedException("Invalid UPI ID. Expected format: name@bank");
        return new Receipt(getMode(), amount, "UPI ID " + upiId);
    }
}
