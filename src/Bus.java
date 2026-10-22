import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Shared bus data and validation. */
public abstract class Bus implements Reservable {
    public static final int MAX_SEATS = 60;

    private String busNo;
    private int totalSeats;
    private int availableSeats;
    private String source;
    private String destination;
    private double farePerSeat;
    private String travelDate;
    private String departureTime;

    /** Creates a bus with every seat available. */
    protected Bus(String busNo, int totalSeats, String source, String destination,
                  double farePerSeat, String travelDate, String departureTime) {
        this(busNo, totalSeats, totalSeats, source, destination, farePerSeat, travelDate, departureTime);
    }

    /** Creates a bus with a stored availability count. */
    protected Bus(String busNo, int totalSeats, int availableSeats, String source, String destination,
                  double farePerSeat, String travelDate, String departureTime) {
        setBusNo(busNo);
        setTotalSeats(totalSeats);
        setAvailableSeats(availableSeats);
        setSource(source);
        setDestination(destination);
        setFarePerSeat(farePerSeat);
        setTravelDate(travelDate);
        setDepartureTime(departureTime);
    }

    public abstract String getBusType();
    /** Returns the charged fare per seat. */
    public abstract double getEffectiveFare();
    public abstract String getAmenities();

    // Getters
    public String getBusNo() { return busNo; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public double getFarePerSeat() { return farePerSeat; }
    public String getTravelDate() { return travelDate; }
    public String getDepartureTime() { return departureTime; }

    // Setters
    public void setBusNo(String busNo) {
        if (busNo == null || !busNo.trim().matches("[A-Za-z0-9-]{3,15}"))
            throw new IllegalArgumentException("Bus number must be 3-15 letters, digits or hyphens.");
        this.busNo = busNo.trim().toUpperCase();
    }

    public void setTotalSeats(int totalSeats) {
        if (totalSeats < 1 || totalSeats > MAX_SEATS)
            throw new IllegalArgumentException("Total seats must be between 1 and " + MAX_SEATS + ".");
        if (availableSeats > totalSeats) availableSeats = totalSeats;
        this.totalSeats = totalSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        if (availableSeats < 0 || availableSeats > totalSeats)
            throw new IllegalArgumentException("Available seats must be between 0 and " + totalSeats + ".");
        this.availableSeats = availableSeats;
    }

    public void setSource(String source) {
        this.source = validCity(source, "Source");
    }

    public void setDestination(String destination) {
        String d = validCity(destination, "Destination");
        if (source != null && d.equalsIgnoreCase(source))
            throw new IllegalArgumentException("Source and destination cannot be the same.");
        this.destination = d;
    }

    public void setFarePerSeat(double farePerSeat) {
        validateFarePerSeat(farePerSeat);
        this.farePerSeat = Math.round(farePerSeat * 100.0) / 100.0;
    }

    static void validateFarePerSeat(double farePerSeat) {
        if (Double.isNaN(farePerSeat) || farePerSeat <= 0 || farePerSeat > 100000)
            throw new IllegalArgumentException("Fare per seat must be greater than 0 (and at most 100000).");
    }

    public void setTravelDate(String travelDate) {
        try {
            LocalDate.parse(travelDate == null ? "" : travelDate.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Travel date must be in yyyy-MM-dd format.");
        }
        this.travelDate = travelDate.trim();
    }

    public void setDepartureTime(String departureTime) {
        try {
            LocalTime.parse(departureTime == null ? "" : departureTime.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Departure time must be in HH:mm format.");
        }
        this.departureTime = departureTime.trim();
    }

    private static String validCity(String city, String label) {
        if (city == null || !city.trim().matches("[A-Za-z][A-Za-z .]{1,29}"))
            throw new IllegalArgumentException(label + " must be 2-30 letters.");
        return city.trim();
    }

    @Override
    public boolean checkAvailability(int seats) {
        return seats > 0 && seats <= availableSeats;
    }

    @Override
    public void confirmBooking(int seats) throws SeatUnavailableException {
        if (seats <= 0) throw new IllegalArgumentException("Seats to book must be positive.");
        if (!checkAvailability(seats))
            throw new SeatUnavailableException("Only " + availableSeats + " seat(s) left on bus " + busNo
                    + " but " + seats + " requested.", seats, availableSeats);
        availableSeats -= seats;
    }

    @Override
    public void cancelBooking(int seats) {
        if (seats <= 0) throw new IllegalArgumentException("Seats to cancel must be positive.");
        availableSeats = Math.min(totalSeats, availableSeats + seats);
    }

    @Override
    public String toString() {
        return String.format("%-9s %-6s %-12s -> %-12s %s %s  seats %2d/%-2d  Rs.%8.2f",
                busNo, getBusType(), source, destination, travelDate, departureTime,
                availableSeats, totalSeats, getEffectiveFare());
    }
}
