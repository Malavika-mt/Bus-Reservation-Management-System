/** Thrown when a booking request is invalid. */
public class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) { super(message); }
    public InvalidBookingException(String message, Throwable cause) { super(message, cause); }
}
