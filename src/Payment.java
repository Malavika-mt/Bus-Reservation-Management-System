/** Base class for payment methods. */
public abstract class Payment {
    /** Processes a payment and returns its receipt. */
    public abstract Receipt processPayment(double amount) throws PaymentFailedException;

    /** Returns the payment mode. */
    public abstract String getMode();

    /** Validates a payment amount. */
    protected void validateAmount(double amount) throws PaymentFailedException {
        if (Double.isNaN(amount) || amount <= 0)
            throw new PaymentFailedException("Payment amount must be greater than zero.");
    }
}
