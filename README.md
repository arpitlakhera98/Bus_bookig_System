# 🚌 Bus Booking System (Java Console Application)

A robust and feature-rich console-based Bus Booking System developed in Java. It handles user bookings, seat management, fare calculation, ticket generation, and administrative controls with local file serialization.

---

## ✨ Features

### 👤 User Features:
- **Search Buses:** Search available buses between cities for a specific journey date.
- **Seat Selection:** View real-time available seat layouts and choose seats (up to 5 seats per booking).
- **Passenger Details:** Input passenger names, ages, and contact info (with mobile & email validation).
- **Fare Breakdown:** Automatic calculation including Base Fare, 5% GST, Platform Fee, Booking Fee, and Coupon code support (`BUS10` for discounts).
- **PNR Tracking:** View ticket details anytime using the unique PNR number.
- **My Bookings:** Look up all bookings linked to a specific mobile number.
- **Cancellation:** Cancel bookings with automated refund tracking.

### 🔐 Admin Features (PIN: `1234`):
- **Bus Management:** View all buses, add new routes, and toggle bus active/inactive status.
- **View All Bookings:** Review all passenger tickets generated in the system.
- **Revenue Summary:** Track total confirmed revenue and recorded cancellation refunds.

---

## 🛠️ Tech Stack
- **Language:** Java
- **Data Persistence:** Java Object Serialization (`.dat` files)
- **APIs / Libraries:** `java.time`, `java.io`, collections, and streams.

---

## 🚀 How to Run the Project

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/AapkaUsername/Bus-Booking-System.git](https://github.com/AapkaUsername/Bus-Booking-System.git)