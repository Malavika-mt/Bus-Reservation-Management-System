/** Cash payment with change calculation. */
public class CashPayment extends Payment {
    private final double tendered;

    public CashPayment(double tendered) { this.tendered = tendered; }

    @Override public String getMode() { return "Cash"; }

    @Override // Method overriding
    public Receipt processPayment(double amount) throws PaymentFailedException {
        validateAmount(amount);
        if (Double.isNaN(tendered) || tendered < amount)
            throw new PaymentFailedException(String.format("Insufficient cash: Rs.%.2f given, Rs.%.2f due.", tendered, amount));
        return new Receipt(getMode(), amount,
                String.format("Cash received Rs.%.2f, change returned Rs.%.2f", tendered, tendered - amount));
    }
}
