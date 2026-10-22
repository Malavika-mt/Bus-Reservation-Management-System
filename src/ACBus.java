/** Air-conditioned bus with a fare surcharge. */
public class ACBus extends Bus {
    public static final double AC_SURCHARGE = 0.20;

    public ACBus(String busNo, int totalSeats, String source, String destination,
                 double farePerSeat, String travelDate, String departureTime) {
        super(busNo, totalSeats, source, destination, farePerSeat, travelDate, departureTime);
    }

    public ACBus(String busNo, int totalSeats, int availableSeats, String source, String destination,
                 double farePerSeat, String travelDate, String departureTime) {
        super(busNo, totalSeats, availableSeats, source, destination, farePerSeat, travelDate, departureTime);
    }

    @Override public String getBusType() { return "AC"; }

    @Override public double getEffectiveFare() {
        return Math.round(getFarePerSeat() * (1 + AC_SURCHARGE) * 100.0) / 100.0;
    }

    @Override public String getAmenities() { return "Air conditioning, charging port, blanket, water bottle"; }
}
