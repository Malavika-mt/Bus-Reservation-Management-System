/** Contract for bookable resources. */
public interface Reservable {
    /** @return true if at least {@code seats} seats are free. */
    boolean checkAvailability(int seats);

    /** Reserves seats; throws if there are not enough. */
    void confirmBooking(int seats) throws SeatUnavailableException;

    /** Releases previously reserved seats. */
    void cancelBooking(int seats);
}
