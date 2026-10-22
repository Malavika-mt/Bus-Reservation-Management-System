/** Thrown when requested seats are unavailable. */
public class SeatUnavailableException extends Exception {
    private final int requested;
    private final int available;

    public SeatUnavailableException(String message, int requested, int available) {
        super(message);
        this.requested = requested;
        this.available = available;
    }

    public SeatUnavailableException(String message) { this(message, 0, 0); }

    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
