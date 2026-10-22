import java.time.LocalDate;

/** Standalone logic and database tests. */
public class TestCases {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    interface Thrower { void run() throws Exception; }

    static boolean throwsType(Class<? extends Throwable> t, Thrower r) {
        try { r.run(); return false; }
        catch (Throwable e) { return t.isInstance(e); }
    }

    public static void main(String[] a) throws Exception {
        DatabaseConnection.initSchema();
        String tag = String.valueOf(System.currentTimeMillis() % 1000000);
        String tomorrow = LocalDate.now().plusDays(1).toString();

        // Users
        check("Encapsulation: bad email rejected", throwsType(IllegalArgumentException.class,
                () -> new Passenger("T1", "Test User", "bad-email", "9123456780", "abc123", 20)));
        check("Encapsulation: weak password rejected", throwsType(IllegalArgumentException.class,
                () -> new Passenger("T1", "Test User", "t@mail.com", "9123456780", "abcdef", 20)));
        check("Encapsulation: bad access level rejected", throwsType(IllegalArgumentException.class,
                () -> new Admin("T2", "Test Admin", "ta@mail.com", "9123456780", "abc123", "Ops", 7)));
        User u = new Passenger("T" + tag, "Test User", "t" + tag + "@mail.com", "9123456780", "abc123", 20);
        check("Password stored hashed + authenticate works", u.authenticate("abc123") && !u.authenticate("wrong1")
                && !u.getPasswordHash().contains("abc123"));
        User ad = new Admin("TA" + tag, "Test Admin", "ta" + tag + "@mail.com", "9123456781", "abc123", "Ops", 1);
        check("Overriding: getRoleDetails differs per role",
                u.getRoleDetails().contains("Passenger") && ad.getRoleDetails().contains("Administrator"));

        // Buses
        Bus ac = new ACBus("T-AC-" + tag, 10, "Aaa", "Bbb", 100, tomorrow, "10:00");
        Bus non = new NonACBus("T-NA-" + tag, 10, "Aaa", "Bbb", 100, tomorrow, "11:00");
        check("Overriding: AC fare 120, NonAC fare 100", ac.getEffectiveFare() == 120.0 && non.getEffectiveFare() == 100.0);
        check("Bus is Reservable", ac instanceof Reservable);
        check("Source == destination rejected", throwsType(IllegalArgumentException.class,
                () -> new NonACBus("T-X-1", 10, "Same", "same", 100, tomorrow, "10:00")));
        check("Zero fare rejected", throwsType(IllegalArgumentException.class,
                () -> new NonACBus("T-X-2", 10, "Aaa", "Bbb", 0, tomorrow, "10:00")));
        ac.confirmBooking(4);
        check("confirmBooking reduces seats", ac.getAvailableSeats() == 6);
        check("Over-booking throws SeatUnavailableException", throwsType(SeatUnavailableException.class, () -> ac.confirmBooking(7)));
        ac.cancelBooking(4);
        check("cancelBooking restores seats (capped)", ac.getAvailableSeats() == 10);

        // Payments
        check("Card: valid card accepted", ok(() -> new CardPayment("4111111111111111", "Test User", "12/35", "123").processPayment(50)));
        check("Card: bad Luhn rejected", throwsType(PaymentFailedException.class, () -> new CardPayment("4111111111111112", "Test User", "12/35", "123").processPayment(50)));
        check("Card: expired rejected", throwsType(PaymentFailedException.class, () -> new CardPayment("4111111111111111", "Test User", "01/20", "123").processPayment(50)));
        check("Card: bad CVV rejected", throwsType(PaymentFailedException.class, () -> new CardPayment("4111111111111111", "Test User", "12/35", "12").processPayment(50)));
        check("UPI: valid id accepted", ok(() -> new UPIPayment("name@okbank").processPayment(50)));
        check("UPI: invalid id rejected", throwsType(PaymentFailedException.class, () -> new UPIPayment("no-at-sign").processPayment(50)));
        check("Cash: insufficient rejected", throwsType(PaymentFailedException.class, () -> new CashPayment(10).processPayment(50)));
        Payment p = new UPIPayment("name@okbank");
        Receipt rc = p.processPayment(75.5);
        check("Receipt generated (txn id, amount, mode)", rc.getTransactionId().startsWith("TXN") && rc.getAmount() == 75.5 && rc.getMode().equals("UPI"));
        check("Polymorphism: Payment ref -> child modes",
                new Payment[]{new UPIPayment("a1@bank"), new CashPayment(9), new CardPayment("4111111111111111", "Test User", "12/35", "123")}[1].getMode().equals("Cash"));

        // Booking service
        BookingService svc = new BookingService();
        svc.selectBus(ac);
        check("calculateFare(3) = 360", svc.calculateFare(3) == 360.0);
        check("calculateFare(3, 10%) = 324", svc.calculateFare(3, 10) == 324.0);
        check("Fare: 7 seats rejected", throwsType(InvalidBookingException.class, () -> svc.calculateFare(7)));
        check("Fare: 150% discount rejected", throwsType(InvalidBookingException.class, () -> svc.calculateFare(2, 150)));
        check("Promo SAVE20 = 20", BookingService.promoPercent("save20") == 20.0);
        check("Bad promo rejected", throwsType(InvalidBookingException.class, () -> BookingService.promoPercent("FAKE")));
        check("searchBus: bad date rejected", throwsType(InvalidBookingException.class, () -> svc.searchBus("Aaa", "Bbb", "31/12/2030")));

        // Database and booking flow
        Passenger pax = (Passenger) u;
        DBUtils.insertUser(pax);
        DBUtils.insertUser(ad);
        User back = DBUtils.findUserByEmail(pax.getEmail());
        check("DB: user round-trip + password still works", back instanceof Passenger && back.authenticate("abc123"));
        check("DB: admin round-trip keeps access level", DBUtils.findUserById(ad.getUserId()) instanceof Admin x && x.getAccessLevel() == 1);
        check("DB: duplicate email rejected by UNIQUE", throwsType(java.sql.SQLException.class,
                () -> DBUtils.insertUser(new Passenger("T9" + tag, "Other", pax.getEmail(), "9123456780", "abc123", 30))));
        DBUtils.insertBus(ac);
        DBUtils.insertBus(non);
        check("DB: bus round-trip is correct subclass", DBUtils.findBus(ac.getBusNo()) instanceof ACBus && DBUtils.findBus(non.getBusNo()) instanceof NonACBus);
        check("DB: search finds the new buses", svc.searchBus("aaa", "BBB", tomorrow).size() == 2 && svc.searchBus("Bbb").size() == 2);

        Reservation r1 = svc.bookTicket(pax, ac.getBusNo(), 4, "WELCOME10", new CashPayment(1000));
        check("Booking: fare 4x120 less 10% = 432", r1.getTotalFare() == 432.0 && r1.getReservationId() > 0);
        check("Booking: seats reduced in DB", DBUtils.findBus(ac.getBusNo()).getAvailableSeats() == 6);
        check("Booking: loyalty points added in DB (4)", ((Passenger) DBUtils.findUserById(pax.getUserId())).getLoyaltyPoints() == 4);

        check("Failed payment rolls back seats", throwsType(PaymentFailedException.class,
                () -> svc.bookTicket(pax, ac.getBusNo(), 2, "", new UPIPayment("bad")))
                && DBUtils.findBus(ac.getBusNo()).getAvailableSeats() == 6);
        check("Booking more than available -> SeatUnavailableException", throwsType(SeatUnavailableException.class,
                () -> { svc.bookTicket(pax, ac.getBusNo(), 6, "", new CashPayment(5000)); svc.bookTicket(pax, ac.getBusNo(), 1, "", new CashPayment(5000)); }));
        // the first inner booking took the last 6 seats (not asserted above), so bus is full now:
        check("Full bus: availableSeats == 0 blocks booking", DBUtils.findBus(ac.getBusNo()).getAvailableSeats() == 0
                && throwsType(SeatUnavailableException.class, () -> svc.bookTicket(pax, ac.getBusNo(), 1, "", new CashPayment(5000))));
        check("Full bus hidden from search", svc.searchBus("Aaa", "Bbb", tomorrow).size() == 1);
        check("Unknown bus -> InvalidBookingException", throwsType(InvalidBookingException.class,
                () -> svc.bookTicket(pax, "NOPE-1", 1, "", new CashPayment(5000))));

        // atomic decrement at DB level can never go below zero
        check("DB decrementSeats refuses to oversell", throwsType(SeatUnavailableException.class, () -> {
            try (var c = DatabaseConnection.getConnection()) { DBUtils.decrementSeats(c, ac.getBusNo(), 1); }
        }));

        // cancellation
        int firstId = r1.getReservationId();
        check("Cancel by another passenger refused", throwsType(InvalidBookingException.class,
                () -> svc.cancelBooking(new Passenger("P999", "Other Pax", "o@mail.com", "9123456780", "abc123", 30), firstId)));
        double refund = svc.cancelBooking(pax, firstId);
        check("Cancel: 90% refund (388.80), seats back", refund == 388.8 && DBUtils.findBus(ac.getBusNo()).getAvailableSeats() == 4);
        check("Double cancel refused", throwsType(InvalidBookingException.class, () -> svc.cancelBooking(pax, firstId)));
        check("Bus with reservations cannot be deleted", throwsType(InvalidBookingException.class, () -> DBUtils.deleteBus(ac.getBusNo())));
        check("User with reservations cannot be deleted", throwsType(InvalidBookingException.class, () -> DBUtils.deleteUser(pax.getUserId())));

        // Race for the final two seats.
        svc.bookTicket(pax, non.getBusNo(), 6, "", new CashPayment(9999));
        svc.bookTicket(pax, non.getBusNo(), 2, "", new CashPayment(9999));
        int[] ok = {0};
        Thread[] ts = new Thread[4];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                try { new BookingService().bookTicket(pax, non.getBusNo(), 1, "", new CashPayment(500)); synchronized (ok) { ok[0]++; } }
                catch (Exception ignored) { }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        check("Concurrency: exactly 2 of 4 racing bookings succeed, no oversell",
                ok[0] == 2 && DBUtils.findBus(non.getBusNo()).getAvailableSeats() == 0);

        cleanup(tag);
        System.out.println("\n" + passed + " passed, " + failed + " failed.");
        System.exit(failed == 0 ? 0 : 1);
    }

    static boolean ok(Thrower t) { try { t.run(); return true; } catch (Exception e) { return false; } }

    static void cleanup(String tag) throws Exception {
        try (var c = DatabaseConnection.getConnection(); var st = c.createStatement()) {
            st.executeUpdate("DELETE FROM reservations WHERE bus_no LIKE 'T-%-" + tag + "'");
            st.executeUpdate("DELETE FROM buses WHERE bus_no LIKE 'T-%-" + tag + "'");
            st.executeUpdate("DELETE FROM users WHERE user_id IN ('T" + tag + "','TA" + tag + "')");
        }
    }
}
