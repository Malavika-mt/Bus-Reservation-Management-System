import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Immutable payment receipt. */
public final class Receipt {
    private final String transactionId;
    private final String mode;
    private final double amount;
    private final String details;
    private final LocalDateTime timestamp;

    public Receipt(String mode, double amount, String details) {
        this.transactionId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.mode = mode;
        this.amount = amount;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getMode() { return mode; }
    public double getAmount() { return amount; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "+----------- PAYMENT RECEIPT -----------+\n"
             + String.format("  Transaction : %s%n", transactionId)
             + String.format("  Mode        : %s%n", mode)
             + String.format("  Amount paid : Rs.%.2f%n", amount)
             + String.format("  Details     : %s%n", details)
             + String.format("  Time        : %s%n", timestamp.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
             + "+---------------------------------------+";
    }
}
