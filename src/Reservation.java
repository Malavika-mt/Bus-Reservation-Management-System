import java.time.LocalDateTime;

/** A passenger ticket booking. */
public class Reservation {
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";

    private int reservationId;
    private final String userId;
    private final String busNo;
    private final int seats;
    private final double totalFare;
    private String status;
    private final LocalDateTime bookedAt;
    private final String paymentMode;
    private final String transactionId;
    // display info (filled when loaded from the database with a JOIN)
    private String route = "";
    private String travelDate = "";

    public Reservation(String userId, String busNo, int seats, double totalFare,
                       String paymentMode, String transactionId) {
        this(0, userId, busNo, seats, totalFare, CONFIRMED, LocalDateTime.now(), paymentMode, transactionId);
    }

    public Reservation(int reservationId, String userId, String busNo, int seats, double totalFare,
                       String status, LocalDateTime bookedAt, String paymentMode, String transactionId) {
        if (seats <= 0) throw new IllegalArgumentException("Seats must be positive.");
        if (totalFare < 0) throw new IllegalArgumentException("Fare cannot be negative.");
        this.reservationId = reservationId;
        this.userId = userId;
        this.busNo = busNo;
        this.seats = seats;
        this.totalFare = totalFare;
        this.status = status;
        this.bookedAt = bookedAt;
        this.paymentMode = paymentMode;
        this.transactionId = transactionId;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int id) { this.reservationId = id; }
    public String getUserId() { return userId; }
    public String getBusNo() { return busNo; }
    public int getSeats() { return seats; }
    public double getTotalFare() { return totalFare; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public String getPaymentMode() { return paymentMode; }
    public String getTransactionId() { return transactionId; }
    public String getRoute() { return route; }
    public String getTravelDate() { return travelDate; }
    public void setDisplayInfo(String route, String travelDate) { this.route = route; this.travelDate = travelDate; }
    public boolean isConfirmed() { return CONFIRMED.equals(status); }

    @Override
    public String toString() {
        return String.format("#%-4d %-9s %-26s %-10s seats:%-2d Rs.%9.2f %-9s %-5s %s",
                reservationId, busNo, route, travelDate, seats, totalFare, status, paymentMode, userId);
    }
}
