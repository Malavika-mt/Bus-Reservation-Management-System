# Bus Reservation System (Java Swing GUI + OOP + JDBC + MySQL)

## Folder layout
- `src/`          all Java source files (default package)
- `lib/`          put `mysql-connector-j-26.7.0.jar` here (download from dev.mysql.com, extract the zip, copy only the .jar)
- `db.properties` local MySQL login (create from `db.properties.example`; keep it in the project root next to `src`)
- `sql/schema.sql` same schema the app creates automatically on first run

## Run in an IDE
1. Make sure MySQL is running.
2. Copy `db.properties.example` to `db.properties` and set your local MySQL user and password.
3. Open/import the project:
   - **Eclipse:** File > Import > General > Existing Projects into Workspace > select this folder (.project/.classpath included).
   - **IntelliJ:** File > Open > this folder; mark `src` as Sources Root; File > Project Structure > Libraries > + > add the jar from `lib/`.
   - **NetBeans:** New Project > Java with Existing Sources; add `src`; add the jar under Libraries.
   - **VS Code:** open this folder with the Java extension pack (the jar in `lib/` is picked up automatically).
4. Run `MainApplication.java` - the Swing window opens (login / register, then the passenger or admin dashboard).
   Presentation demo (prints every OOP concept in the console): add program argument `--demo`. Logic tests: run `TestCases.java`.
Working directory must be the project root so `db.properties` is found.

Default admin: admin@bus.com / admin123 (admin registration code: BUS-ADMIN)
Promo codes: WELCOME10, FESTIVE15, SAVE20. Test card 4111 1111 1111 1111 (future expiry, any 3-digit CVV). Test UPI: name@okbank.
`UserApp.java` is Member 1's original standalone Swing screen (in-memory, not connected to the database); `MainApplication` is the full app and reuses its User/Passenger/Admin classes.

## OOP map
| Member | Files | Concepts |
|---|---|---|
| 1 | User, Passenger, Admin | Encapsulation, inheritance, overriding getRoleDetails() |
| 2 | Reservable, Bus, ACBus, NonACBus | Interface, abstraction, overriding getEffectiveFare() |
| 3 | BookingService, Reservation | Overloading: 3x searchBus, 2x calculateFare |
| 4 | Payment, CardPayment, UPIPayment, CashPayment, Receipt | Abstraction, overriding processPayment() |
| 5 | DatabaseConnection, DBUtils, SeatUnavailableException, InvalidBookingException, PaymentFailedException | JDBC, custom exceptions, no overselling |
| 6 | MainApplication (login/register frame), AdminConsole + PassengerConsole (Swing dashboards), Ui (helpers), TestCases | Runtime polymorphism (`User`, `Payment`, `Bus` parent references), GUI, demo, tests |

## Using the window
- **Passenger:** Search & Book (leave boxes blank to list everything; the number of boxes filled picks the 1-, 2- or 3-argument `searchBus`), select a bus, choose seats + optional promo, **Book & Pay** (Card / UPI / Cash). My Bookings (cancel), My Role, My Profile.
- **Admin:** Buses (add / update fare / remove), Reservations (cancel any), Users (remove), My Role. Higher access levels unlock more buttons.
- Sample buses whose date has passed (and have no active bookings) are moved forward automatically on start-up.
