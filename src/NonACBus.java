/** Non-air-conditioned bus. */
public class NonACBus extends Bus {
    public NonACBus(String busNo, int totalSeats, String source, String destination,
                    double farePerSeat, String travelDate, String departureTime) {
        super(busNo, totalSeats, source, destination, farePerSeat, travelDate, departureTime);
    }

    public NonACBus(String busNo, int totalSeats, int availableSeats, String source, String destination,
                    double farePerSeat, String travelDate, String departureTime) {
        super(busNo, totalSeats, availableSeats, source, destination, farePerSeat, travelDate, departureTime);
    }

    @Override public String getBusType() { return "NonAC"; }

    @Override public double getEffectiveFare() { return getFarePerSeat(); }

    @Override public String getAmenities() { return "Pushback seats, window ventilation"; }
}
