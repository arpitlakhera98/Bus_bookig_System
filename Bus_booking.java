package projects;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Bus Booking System - Java console project.
 * Stores buses and bookings in local serialized files.
 *
 * Demo admin PIN: 1234
 */
public class Bus_booking {
    private static final Scanner input = new Scanner(System.in);
    private static final String BUS_FILE = "buses.dat";
    private static final String BOOKING_FILE = "bookings.dat";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static List<Bus> buses = new ArrayList<>();
    private static List<Booking> bookings = new ArrayList<>();

    public static void main(String[] args) {
        loadData();
        seedBusesIfEmpty();

        while (true) {
            System.out.println("\n========== BUS BOOKING SYSTEM ==========");
            System.out.println("1. Search buses and book tickets");
            System.out.println("2. View booking by PNR");
            System.out.println("3. View all my bookings (mobile number)");
            System.out.println("4. Cancel booking");
            System.out.println("5. List available buses");
            System.out.println("6. Admin menu");
            System.out.println("0. Exit");

            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> searchAndBook();
                case 2 -> viewBooking();
                case 3 -> viewBookingsByMobile();
                case 4 -> cancelBooking();
                case 5 -> listBuses();
                case 6 -> adminMenu();
                case 0 -> {
                    saveData();
                    System.out.println("Data saved. Thank you for using Bus Booking System!");
                    input.close();
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    // -------------------- BUS SEARCH & BOOKING --------------------

    private static void searchAndBook() {
        System.out.println("\n--- Search buses ---");
        String source = readNonBlank("From city: ");
        String destination = readNonBlank("To city: ");

        if (source.equalsIgnoreCase(destination)) {
            System.out.println("Source and destination cannot be the same.");
            return;
        }

        LocalDate journeyDate = readDate("Journey date (dd-MM-yyyy): ");
        if (journeyDate.isBefore(LocalDate.now())) {
            System.out.println("Journey date cannot be in the past.");
            return;
        }

        List<Bus> matches = new ArrayList<>();
        for (Bus bus : buses) {
            if (bus.source.equalsIgnoreCase(source)
                    && bus.destination.equalsIgnoreCase(destination)
                    && bus.active) {
                matches.add(bus);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No buses found for this route. Check available routes or ask admin to add a bus.");
            return;
        }

        System.out.println("\nAvailable buses for " + journeyDate.format(DATE_FORMAT));
        for (int i = 0; i < matches.size(); i++) {
            Bus bus = matches.get(i);
            System.out.printf("%d. %s | %s | Departure %s | Arrival %s | %s | Rs. %.2f/seat | Available: %d%n",
                    i + 1, bus.name, bus.type, bus.departure, bus.arrival,
                    bus.source + " -> " + bus.destination, bus.fare,
                    countAvailableSeats(bus, journeyDate));
        }

        int selected = readInt("Select bus number (0 to go back): ");
        if (selected == 0) return;
        if (selected < 1 || selected > matches.size()) {
            System.out.println("Invalid bus selection.");
            return;
        }

        Bus bus = matches.get(selected - 1);
        List<Integer> available = availableSeatNumbers(bus, journeyDate);
        if (available.isEmpty()) {
            System.out.println("Sorry, this bus is fully booked on that date.");
            return;
        }

        int passengerCount = readInt("Number of passengers (1-" + Math.min(5, available.size()) + "): ");
        if (passengerCount < 1 || passengerCount > 5 || passengerCount > available.size()) {
            System.out.println("Invalid passenger count or not enough seats.");
            return;
        }

        System.out.println("Available seat numbers: " + available);
        List<Integer> chosenSeats = new ArrayList<>();
        while (chosenSeats.size() < passengerCount) {
            int seat = readInt("Choose seat for passenger " + (chosenSeats.size() + 1) + ": ");
            if (!available.contains(seat)) {
                System.out.println("That seat is unavailable or invalid.");
            } else if (chosenSeats.contains(seat)) {
                System.out.println("You already selected that seat.");
            } else {
                chosenSeats.add(seat);
            }
        }

        List<Passenger> passengers = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            System.out.println("\nPassenger " + (i + 1));
            String name = readNonBlank("Full name: ");
            int age = readInt("Age: ");
            while (age < 1 || age > 120) {
                System.out.println("Enter a valid age from 1 to 120.");
                age = readInt("Age: ");
            }
            passengers.add(new Passenger(name, age, chosenSeats.get(i)));
        }

        String mobile = readNonBlank("Contact mobile number: ");
        while (!mobile.matches("[0-9]{10}")) {
            System.out.println("Enter a valid 10-digit Indian mobile number.");
            mobile = readNonBlank("Contact mobile number: ");
        }
        String email = readNonBlank("Email address: ");
        while (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            System.out.println("Enter a valid email address.");
            email = readNonBlank("Email address: ");
        }

        double baseFare = bus.fare * passengerCount;
        double gst = baseFare * 0.05;
        double platformFee = 9.87;
        double bookingFee = 6.45;
        double subtotal = baseFare + gst + platformFee + bookingFee;
        double discount = 0;

        String coupon = readLine("Coupon code (press Enter to skip): ").trim().toUpperCase(Locale.ROOT);
        if (coupon.equals("BUS10")) {
            discount = Math.min(baseFare * 0.10, 300);
            System.out.printf("BUS10 applied. Discount: Rs. %.2f%n", discount);
        } else if (!coupon.isEmpty()) {
            System.out.println("Coupon not recognised; no discount applied.");
        }

        double total = Math.max(0, subtotal - discount);
        System.out.println("\n--- Fare summary ---");
        System.out.printf("Base fare: Rs. %.2f%n", baseFare);
        System.out.printf("GST (5%%): Rs. %.2f%n", gst);
        System.out.printf("Platform fee: Rs. %.2f%n", platformFee);
        System.out.printf("Booking fee: Rs. %.2f%n", bookingFee);
        System.out.printf("Discount: -Rs. %.2f%n", discount);
        System.out.printf("TOTAL: Rs. %.2f%n", total);

        String confirm = readNonBlank("Confirm booking? (Y/N): ");
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Booking cancelled before confirmation.");
            return;
        }

        // Re-check availability before committing the booking.
        List<Integer> latestAvailable = availableSeatNumbers(bus, journeyDate);
        if (!latestAvailable.containsAll(chosenSeats)) {
            System.out.println("One or more seats have just become unavailable. Please try again.");
            return;
        }

        String pnr = generateUniquePnr();
        Booking booking = new Booking(pnr, bus.id, bus.name, bus.source, bus.destination,
                journeyDate, passengers, mobile, email, baseFare, gst,
                platformFee, bookingFee, discount, total, "CONFIRMED",
                LocalDateTime.now().toString());
        bookings.add(booking);
        saveData();

        System.out.println("\nBooking successful!");
        printTicket(booking);
    }

    // -------------------- BOOKINGS --------------------

    private static void viewBooking() {
        String pnr = readNonBlank("Enter PNR: ").trim().toUpperCase(Locale.ROOT);
        Booking booking = findBooking(pnr);
        if (booking == null) {
            System.out.println("No booking found with that PNR.");
        } else {
            printTicket(booking);
        }
    }

    private static void viewBookingsByMobile() {
        String mobile = readNonBlank("Enter contact mobile number: ");
        boolean found = false;
        for (Booking booking : bookings) {
            if (booking.mobile.equals(mobile)) {
                printTicket(booking);
                found = true;
            }
        }
        if (!found) System.out.println("No bookings found for that mobile number.");
        System.out.println("Note: In a real system, protect this lookup with user authentication/OTP.");
    }

    private static void cancelBooking() {
        String pnr = readNonBlank("Enter PNR to cancel: ").trim().toUpperCase(Locale.ROOT);
        Booking booking = findBooking(pnr);
        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }
        if (!booking.status.equals("CONFIRMED")) {
            System.out.println("This booking is already " + booking.status + ".");
            return;
        }

        System.out.println("Cancellation policy: demo refund is 90% of ticket base fare; fees and GST are not refunded.");
        System.out.printf("Estimated refund: Rs. %.2f%n", booking.baseFare * 0.90);
        String confirm = readNonBlank("Confirm cancellation? (Y/N): ");
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        booking.status = "CANCELLED";
        booking.refundAmount = booking.baseFare * 0.90;
        booking.cancelledAt = LocalDateTime.now().toString();
        saveData();
        System.out.printf("Booking cancelled. Demo refund amount: Rs. %.2f%n", booking.refundAmount);
        System.out.println("This is only a recorded demo refund; no real payment was processed.");
    }

    private static void printTicket(Booking b) {
        System.out.println("\n================ E-TICKET ================");
        System.out.println("PNR / Booking ID : " + b.pnr);
        System.out.println("Status           : " + b.status);
        System.out.println("Bus              : " + b.busName + " (ID " + b.busId + ")");
        System.out.println("Route            : " + b.source + " -> " + b.destination);
        System.out.println("Journey date     : " + b.journeyDate.format(DATE_FORMAT));
        System.out.println("Booked on        : " + b.createdAt.replace('T', ' '));
        System.out.println("------------------------------------------");
        for (Passenger p : b.passengers) {
            System.out.printf("%-22s Age: %-3d Seat: %s%d%n",
                    p.name, p.age, seatRow(p.seatNumber), seatCol(p.seatNumber));
        }
        System.out.println("------------------------------------------");
        System.out.println("Contact mobile   : " + b.mobile);
        System.out.println("Contact email    : " + b.email);
        System.out.printf("Base fare        : Rs. %.2f%n", b.baseFare);
        System.out.printf("GST              : Rs. %.2f%n", b.gst);
        System.out.printf("Platform fee     : Rs. %.2f%n", b.platformFee);
        System.out.printf("Booking fee      : Rs. %.2f%n", b.bookingFee);
        System.out.printf("Discount         : Rs. %.2f%n", b.discount);
        System.out.printf("Total paid (demo): Rs. %.2f%n", b.total);
        if (b.status.equals("CANCELLED")) {
            System.out.printf("Recorded refund  : Rs. %.2f%n", b.refundAmount);
        }
        System.out.println("==========================================");
    }

    // -------------------- ADMIN --------------------

    private static void adminMenu() {
        String pin = readNonBlank("Admin PIN: ");
        if (!pin.equals("1234")) {
            System.out.println("Incorrect PIN.");
            return;
        }

        while (true) {
            System.out.println("\n------------- ADMIN MENU -------------");
            System.out.println("1. List buses");
            System.out.println("2. Add bus");
            System.out.println("3. Activate/deactivate bus");
            System.out.println("4. View all bookings");
            System.out.println("5. Revenue summary");
            System.out.println("0. Back");
            int choice = readInt("Choose option: ");

            switch (choice) {
                case 1 -> listBuses();
                case 2 -> addBus();
                case 3 -> toggleBus();
                case 4 -> {
                    if (bookings.isEmpty()) System.out.println("No bookings yet.");
                    for (Booking b : bookings) printTicket(b);
                }
                case 5 -> revenueSummary();
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void addBus() {
        System.out.println("\n--- Add bus ---");
        String name = readNonBlank("Bus/operator name: ");
        String source = readNonBlank("Source city: ");
        String destination = readNonBlank("Destination city: ");
        if (source.equalsIgnoreCase(destination)) {
            System.out.println("Source and destination cannot be the same.");
            return;
        }
        String type;
        while (true) {
            type = readNonBlank("Type (AC / NON-AC / SLEEPER): ").toUpperCase(Locale.ROOT);
            if (type.equals("AC") || type.equals("NON-AC") || type.equals("SLEEPER")) break;
            System.out.println("Choose AC, NON-AC, or SLEEPER.");
        }
        double fare = readDouble("Fare per seat (Rs.): ");
        while (fare <= 0) {
            System.out.println("Fare must be greater than zero.");
            fare = readDouble("Fare per seat (Rs.): ");
        }
        String departure = readTime("Departure time (HH:mm): ");
        String arrival = readTime("Arrival time (HH:mm): ");
        int capacity = readInt("Seat capacity (1-60): ");
        while (capacity < 1 || capacity > 60) {
            System.out.println("Capacity must be between 1 and 60.");
            capacity = readInt("Seat capacity (1-60): ");
        }

        int id = buses.stream().mapToInt(bus -> bus.id).max().orElse(100) + 1;
        buses.add(new Bus(id, name, source, destination, type, departure, arrival, fare, capacity, true));
        saveData();
        System.out.println("Bus added with ID " + id + ".");
    }

    private static void toggleBus() {
        listBuses();
        int id = readInt("Enter bus ID: ");
        Bus bus = findBus(id);
        if (bus == null) {
            System.out.println("Bus not found.");
            return;
        }
        bus.active = !bus.active;
        saveData();
        System.out.println("Bus " + id + " is now " + (bus.active ? "active" : "inactive") + ".");
    }

    private static void revenueSummary() {
        double total = 0;
        double refunded = 0;
        int confirmed = 0;
        int cancelled = 0;
        for (Booking b : bookings) {
            if (b.status.equals("CONFIRMED")) {
                total += b.total;
                confirmed++;
            } else if (b.status.equals("CANCELLED")) {
                refunded += b.refundAmount;
                cancelled++;
            }
        }
        System.out.println("\n--- Revenue summary (demo records) ---");
        System.out.println("Confirmed bookings: " + confirmed);
        System.out.println("Cancelled bookings: " + cancelled);
        System.out.printf("Confirmed booking totals: Rs. %.2f%n", total);
        System.out.printf("Recorded refunds: Rs. %.2f%n", refunded);
    }

    private static void listBuses() {
        System.out.println("\n--- Bus list ---");
        if (buses.isEmpty()) {
            System.out.println("No buses configured.");
            return;
        }
        for (Bus b : buses) {
            System.out.printf("ID %d | %s | %s -> %s | %s | %s-%s | Rs. %.2f | Seats: %d | %s%n",
                    b.id, b.name, b.source, b.destination, b.type,
                    b.departure, b.arrival, b.fare, b.capacity,
                    b.active ? "ACTIVE" : "INACTIVE");
        }
    }

    // -------------------- SEATS & DATA --------------------

    private static int countAvailableSeats(Bus bus, LocalDate date) {
        return availableSeatNumbers(bus, date).size();
    }

    private static List<Integer> availableSeatNumbers(Bus bus, LocalDate date) {
        Set<Integer> booked = new HashSet<>();
        for (Booking booking : bookings) {
            if (booking.busId == bus.id
                    && booking.journeyDate.equals(date)
                    && booking.status.equals("CONFIRMED")) {
                for (Passenger passenger : booking.passengers) booked.add(passenger.seatNumber);
            }
        }
        List<Integer> available = new ArrayList<>();
        for (int seat = 1; seat <= bus.capacity; seat++) {
            if (!booked.contains(seat)) available.add(seat);
        }
        return available;
    }

    private static String seatRow(int seat) {
        return String.valueOf((char) ('A' + (seat - 1) / 4));
    }

    private static int seatCol(int seat) {
        return ((seat - 1) % 4) + 1;
    }

    private static String generateUniquePnr() {
        String pnr;
        do {
            pnr = "BUS" + (100000 + new Random().nextInt(900000));
        } while (findBooking(pnr) != null);
        return pnr;
    }

    private static Booking findBooking(String pnr) {
        for (Booking b : bookings) if (b.pnr.equalsIgnoreCase(pnr)) return b;
        return null;
    }

    private static Bus findBus(int id) {
        for (Bus b : buses) if (b.id == id) return b;
        return null;
    }

    private static void seedBusesIfEmpty() {
        if (!buses.isEmpty()) return;
        buses.add(new Bus(101, "Express Travels", "Delhi", "Jaipur", "AC",
                "08:00", "13:00", 800, 40, true));
        buses.add(new Bus(102, "RoadStar", "Delhi", "Jaipur", "NON-AC",
                "09:30", "15:00", 500, 40, true));
        buses.add(new Bus(103, "Night Rider", "Delhi", "Agra", "SLEEPER",
                "22:00", "02:30", 650, 32, true));
        buses.add(new Bus(104, "CityLink", "Jaipur", "Delhi", "AC",
                "16:00", "21:00", 750, 40, true));
        saveData();
    }

    private static void loadData() {
        Object busObject = readObjectFile(BUS_FILE, BusListWrapper.class);
        if (busObject instanceof BusListWrapper wrapper) {
            buses = wrapper.items;
        } else {
            buses = new ArrayList<>();
        }
        bookings = readBookings(BOOKING_FILE);
    }

    private static Object readObjectFile(String file, Class<?> expected) {
        File f = new File(file);
        if (!f.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = in.readObject();
            if (expected.isInstance(obj)) return obj;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not load " + file + ": " + e.getMessage());
        }
        return null;
    }

    private static List<Booking> readBookings(String file) {
        Object obj = readObjectFile(file, BookingListWrapper.class);
        if (obj instanceof BookingListWrapper wrapper) return wrapper.items;
        return new ArrayList<>();
    }

    private static void saveData() {
        writeObjectFile(BUS_FILE, new BusListWrapper(buses));
        writeObjectFile(BOOKING_FILE, new BookingListWrapper(bookings));
    }

    private static void writeObjectFile(String file, Object obj) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(obj);
        } catch (IOException e) {
            System.out.println("Could not save " + file + ": " + e.getMessage());
        }
    }

    // -------------------- INPUT HELPERS --------------------

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim();
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return input.nextLine();
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            String value = readNonBlank(prompt);
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (Exception e) {
                System.out.println("Invalid date. Example: 31-12-2026");
            }
        }
    }

    private static String readTime(String prompt) {
        while (true) {
            String value = readNonBlank(prompt);
            try {
                java.time.LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
                return value;
            } catch (Exception e) {
                System.out.println("Invalid time. Use 24-hour HH:mm format, e.g. 08:30.");
            }
        }
    }

    // -------------------- SERIALIZABLE MODELS --------------------

    private static class Bus implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name, source, destination, type, departure, arrival;
        double fare;
        int capacity;
        boolean active;

        Bus(int id, String name, String source, String destination, String type,
            String departure, String arrival, double fare, int capacity, boolean active) {
            this.id = id;
            this.name = name;
            this.source = source;
            this.destination = destination;
            this.type = type;
            this.departure = departure;
            this.arrival = arrival;
            this.fare = fare;
            this.capacity = capacity;
            this.active = active;
        }
    }

    private static class Passenger implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int age;
        int seatNumber;

        Passenger(String name, int age, int seatNumber) {
            this.name = name;
            this.age = age;
            this.seatNumber = seatNumber;
        }
    }

    private static class Booking implements Serializable {
        private static final long serialVersionUID = 1L;
        String pnr, busName, source, destination, mobile, email, status, createdAt, cancelledAt;
        int busId;
        LocalDate journeyDate;
        List<Passenger> passengers;
        double baseFare, gst, platformFee, bookingFee, discount, total, refundAmount;

        Booking(String pnr, int busId, String busName, String source, String destination,
                LocalDate journeyDate, List<Passenger> passengers, String mobile, String email,
                double baseFare, double gst, double platformFee, double bookingFee,
                double discount, double total, String status, String createdAt) {
            this.pnr = pnr;
            this.busId = busId;
            this.busName = busName;
            this.source = source;
            this.destination = destination;
            this.journeyDate = journeyDate;
            this.passengers = new ArrayList<>(passengers);
            this.mobile = mobile;
            this.email = email;
            this.baseFare = baseFare;
            this.gst = gst;
            this.platformFee = platformFee;
            this.bookingFee = bookingFee;
            this.discount = discount;
            this.total = total;
            this.status = status;
            this.createdAt = createdAt;
            this.refundAmount = 0;
            this.cancelledAt = "";
        }
    }

    private static class BusListWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        ArrayList<Bus> items;
        BusListWrapper(List<Bus> items) { this.items = new ArrayList<>(items); }
    }

    private static class BookingListWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        ArrayList<Booking> items;
        BookingListWrapper(List<Booking> items) { this.items = new ArrayList<>(items); }
    }
}
