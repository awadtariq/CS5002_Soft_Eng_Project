import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the business logic for managing bookings.
 * This is the "Controller" that bridges the UI and the Data.
 */
public class BookingManager {
    private static final List<Booking> allBookings = new ArrayList<>();

    static {
        // Initialise with seed data (British English spelling)
        allBookings.add(new Booking("BK001", "alice", "Jazz Night", "2026-04-10", "Confirmed", "£45.00"));
        allBookings.add(new Booking("BK002", "bob", "Art Expo", "2026-04-15", "Expired", "£30.00"));
    }

    public static List<Booking> getAllBookings() {
        return allBookings;
    }

    public static void addBooking(Booking b) {
        allBookings.add(b);
    }

    public static List<Booking> getBookingsForUser(String username, boolean isAdmin) {
        if (isAdmin) return allBookings;
        return allBookings.stream()
                .filter(b -> b.getUsername().equals(username))
                .collect(Collectors.toList());
    }

    public static int getNextId() {
        return allBookings.size() + 1;
    }
}