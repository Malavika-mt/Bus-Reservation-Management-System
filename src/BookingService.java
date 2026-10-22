import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Handles bus search, fares, and transactional bookings. */
public class BookingService {
    public static final int MAX_SEATS_PER_BOOKING = 6;
    public static final double CANCELLATION_FEE_PERCENT = 10;

    private static final Map<String, Double> PROMO_CODES = Map.of("WELCOME10", 10.0, "SAVE20", 20.0, "FESTIVE15", 15.0);

    private Bus selectedBus; // the bus the fare methods work on

    // Search
    public List<Bus> searchBus(String destination) throws SQLException {
        return DBUtils.searchBuses(null, destination, null);
    }

    public List<Bus> searchBus(String source, String destination) throws SQLException {
        return DBUtils.searchBuses(source, destination, null);
    }

    public List<Bus> searchBus(String source, String destination, String travelDate)
            throws SQLException, InvalidBookingException {
        try {
            LocalDate.parse(travelDate.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidBookingException("Travel date must be in yyyy-MM-dd format.");
        }
        return DBUtils.searchBuses(source, destination, travelDate);
    }

    // Fare calculation
    public void selectBus(Bus bus) { this.selectedBus = bus; }

    public double calculateFare(int seatCount) throws InvalidBookingException {
        return calculateFare(seatCount, 0);
    }

    /** @param promoDiscount percentage off, 0-100 */
    public double calculateFare(int seatCount, double promoDiscount) throws InvalidBookingException {
        if (selectedBus == null) throw new InvalidBookingException("No bus selected.");
        if (seatCount < 1 || seatCount > MAX_SEATS_PER_BOOKING)
            throw new InvalidBookingException("You can book 1 to " + MAX_SEATS_PER_BOOKING + " seats at a time.");
        if (Double.isNaN(promoDiscount) || promoDiscount < 0 || promoDiscount > 100)
            throw new InvalidBookingException("Discount must be between 0 and 100 percent.");
        double fare = selectedBus.getEffectiveFare() * seatCount * (1 - promoDiscount / 100.0);
        return Math.round(fare * 100.0) / 100.0;
    }

    /** Returns the discount percent for a promo code (blank = 0). */
    public static double promoPercent(String code) throws InvalidBookingException {
        if (code == null || code.isBlank()) return 0;
        Double pct = PROMO_CODES.get(code.trim().toUpperCase());
        if (pct == null) throw new InvalidBookingException("Invalid promo code: " + code.trim());
        return pct;
    }

    /** Price quote for the console before paying. */
    public double quote(String busNo, int seats, String promoCode)
            throws SQLException, InvalidBookingException, SeatUnavailableException {
        Bus bus = requireBookableBus(busNo, seats);
        selectBus(bus);
        return calculateFare(seats, promoPercent(promoCode));
    }

    /** Books seats and payment atomically. */
    public Reservation bookTicket(Passenger passenger, String busNo, int seats, String promoCode, Payment payment)
            throws SQLException, InvalidBookingException, SeatUnavailableException, PaymentFailedException {
        if (passenger == null || payment == null) throw new InvalidBookingException("Passenger and payment are required.");
        Bus bus = requireBookableBus(busNo, seats);
        selectBus(bus);
        double fare = calculateFare(seats, promoPercent(promoCode));

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                DBUtils.findBus(c, bus.getBusNo(), true);
                DBUtils.decrementSeats(c, bus.getBusNo(), seats);
                Receipt receipt = payment.processPayment(fare);
                Reservation r = new Reservation(passenger.getUserId(), bus.getBusNo(), seats, fare,
                        receipt.getMode(), receipt.getTransactionId());
                r.setReservationId(DBUtils.insertReservation(c, r));
                int points = (int) (fare / 100);
                if (points > 0) DBUtils.addLoyaltyPoints(c, passenger.getUserId(), points);
                c.commit();
                if (points > 0) passenger.addLoyaltyPoints(points);
                r.setDisplayInfo(bus.getSource() + " -> " + bus.getDestination(), bus.getTravelDate());
                lastReceipt = receipt;
                return r;
            } catch (SQLException | SeatUnavailableException | PaymentFailedException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Receipt lastReceipt;
    public Receipt getLastReceipt() { return lastReceipt; }

    /** Cancels a booking and returns the refund amount. */
    public double cancelBooking(User requester, int reservationId)
            throws SQLException, InvalidBookingException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Reservation r = DBUtils.findReservation(c, reservationId, true);
                if (r == null) throw new InvalidBookingException("Reservation #" + reservationId + " not found.");
                if (requester instanceof Passenger && !r.getUserId().equals(requester.getUserId()))
                    throw new InvalidBookingException("You can only cancel your own bookings.");
                if (!r.isConfirmed()) throw new InvalidBookingException("Reservation #" + reservationId + " is already cancelled.");
                if (LocalDate.parse(r.getTravelDate()).isBefore(LocalDate.now()))
                    throw new InvalidBookingException("Journey date has passed; this booking cannot be cancelled.");

                DBUtils.markCancelled(c, reservationId);
                DBUtils.restoreSeats(c, r.getBusNo(), r.getSeats());
                int points = (int) (r.getTotalFare() / 100);
                if (points > 0) DBUtils.addLoyaltyPoints(c, r.getUserId(), -points);
                c.commit();
                return Math.round(r.getTotalFare() * (100 - CANCELLATION_FEE_PERCENT)) / 100.0;
            } catch (SQLException | InvalidBookingException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Bus requireBookableBus(String busNo, int seats)
            throws SQLException, InvalidBookingException, SeatUnavailableException {
        if (seats < 1 || seats > MAX_SEATS_PER_BOOKING)
            throw new InvalidBookingException("You can book 1 to " + MAX_SEATS_PER_BOOKING + " seats at a time.");
        Bus bus = busNo == null ? null : DBUtils.findBus(busNo);
        if (bus == null) throw new InvalidBookingException("Bus " + busNo + " does not exist.");
        if (LocalDate.parse(bus.getTravelDate()).isBefore(LocalDate.now()))
            throw new InvalidBookingException("Bus " + bus.getBusNo() + " has already departed.");
        if (!bus.checkAvailability(seats))
            throw new SeatUnavailableException(bus.getAvailableSeats() == 0 ? "Bus " + bus.getBusNo() + " is fully booked."
                    : "Only " + bus.getAvailableSeats() + " seat(s) available on bus " + bus.getBusNo() + ".",
                    seats, bus.getAvailableSeats());
        return bus;
    }
}
