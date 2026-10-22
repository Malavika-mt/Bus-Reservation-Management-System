import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Card payment with number, expiry, and CVV validation. */
public class CardPayment extends Payment {
    private final String cardNumber;
    private final String holderName;
    private final String expiry; // MM/yy
    private final String cvv;

    public CardPayment(String cardNumber, String holderName, String expiry, String cvv) {
        this.cardNumber = cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
        this.holderName = holderName == null ? "" : holderName.trim();
        this.expiry = expiry == null ? "" : expiry.trim();
        this.cvv = cvv == null ? "" : cvv.trim();
    }

    @Override public String getMode() { return "Card"; }

    @Override // Method overriding
    public Receipt processPayment(double amount) throws PaymentFailedException {
        validateAmount(amount);
        if (!holderName.matches("[A-Za-z][A-Za-z .]{1,49}"))
            throw new PaymentFailedException("Invalid card holder name.");
        if (!cardNumber.matches("\\d{13,19}") || !luhnValid(cardNumber))
            throw new PaymentFailedException("Invalid card number.");
        if (!cvv.matches("\\d{3,4}"))
            throw new PaymentFailedException("CVV must be 3 or 4 digits.");
        try {
            YearMonth exp = YearMonth.parse(expiry, DateTimeFormatter.ofPattern("MM/yy"));
            if (exp.isBefore(YearMonth.now())) throw new PaymentFailedException("Card has expired.");
        } catch (DateTimeParseException e) {
            throw new PaymentFailedException("Expiry must be in MM/yy format.");
        }
        String masked = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        return new Receipt(getMode(), amount, "Card " + masked + " (" + holderName + ")");
    }

    /** Luhn checksum used by real card networks. */
    static boolean luhnValid(String number) {
        int sum = 0;
        boolean dbl = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (dbl) { d *= 2; if (d > 9) d -= 9; }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
