# 🏨 Grand Hotel Management System (Executive Edition)

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
![iText](https://img.shields.io/badge/PDF_Generation-iText-red?style=for-the-badge)
![UI](https://img.shields.io/badge/Interface-Extreme_Dark_Mode-blueviolet?style=for-the-badge)

> A comprehensive, enterprise-grade Hotel Management System featuring a modern "Midnight" dashboard interface, live revenue tracking, automated PDF invoicing, and tax-compliant billing logic.

![Project Dashboard Preview]( -<img width="1711" height="1018" alt="Screenshot 2025-12-16 234133" src="https://github.com/user-attachments/assets/130b9857-bb29-4af6-99ed-1b3ba9ae3171" />)

## ✨ Key Features

### 🖥️ Extreme UI / UX
- **Midnight Dark Theme:** Professional color palette (Gunmetal Grey & Neon Accents).
- **Custom Components:** Glowing buttons, floating cards, and glass-morphism tables.
- **Live Dashboard:** Real-time statistics for Total Rooms, Occupancy, and Availability.

### 💼 Core Functionality
- **Smart Booking:** Room previews with images and descriptions before booking.
- **Guest Management:** Check-In/Check-Out with Aadhaar masking (`XXXX-XXXX-1234`).
- **Dynamic Billing:** - Automated calculation of Days, Food Charges, and Room Rent.
  - **GST Logic:** Smart tax calculation (12% vs 18%) based on room price.
  - **Payment Modes:** Cash and Google Pay (with QR Code integration).

### 📊 Analytics & Reporting
- **Revenue Engine:** Tracks every transaction in a hidden history database.
- **Financial Reports:** View "Today's Revenue" vs "Monthly Revenue" instantly.
- **PDF Export:** Generates professional, print-ready PDF invoices using iText.

## 🛠️ Tech Stack

- **Language:** Java (JDK 17+)
- **GUI:** Swing (Custom Painted Components)
- **Database:** MySQL (JDBC Connectivity)
- **Libraries:** - `mysql-connector-java` (DB Connection)
    - `itextpdf-5.5.13.2` (PDF Generation)

## ⚙️ Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone [https://github.com/yourusername/grand-hotel-system.git](https://github.com/yourusername/grand-hotel-system.git)
   Database SetupOpen your MySQL Workbench and run:SQLCREATE DATABASE hoteldb;
2.**Dtabase Setup**

Open **MySQL Workbench** (or your preferred SQL tool) and run the following script to initialize the database:
  ```sql
CREATE DATABASE hoteldb;
USE hoteldb;

-- Create the main Rooms table
CREATE TABLE rooms (
    roomNumber INT PRIMARY KEY,
    type VARCHAR(20),
    pricePerDay DOUBLE,
    isBooked BOOLEAN DEFAULT FALSE,
    customerName VARCHAR(50),
    customerPhone VARCHAR(20),
    daysStayed INT DEFAULT 0,
    bookingDate DATETIME,
    foodOrdered BOOLEAN DEFAULT FALSE,
    aadhaar_last4 VARCHAR(20),
    feedback VARCHAR(255)
);

-- The 'hotel_history' table will be created automatically by the Java application when you run it.

-- Insert dummy room data to start
INSERT INTO rooms (roomNumber, type, pricePerDay) VALUES 
  (101, 'Standard', 2500), 
  (102, 'Deluxe', 4500), 
  (201, 'Suite', 7500);
```

3.**Add Libraries**

In Eclipse, right-click your project > Build Path > Configure Build Path > Libraries > Add External JARs.
Add the following files:
 - ** mysql-connector-j-x.x.x.jar
 - **itextpdf-5.5.13.2.jar

4.**Run the Application**

Run demojdbc.java as a Java Application.

### 🤝 Contributing

1.Contributions, issues, and feature requests are welcome!
2.Fork the Project
3.Create your Feature Branch (git checkout -b feature/AmazingFeature)
4.Commit your Changes (git commit -m 'Add some AmazingFeature')
5.Push to the Branch (git push origin feature/AmazingFeature)
6.Open a Pull Request



### 📝 License

Distributed under the MIT License. See LICENSE for more information.

---

### ⚠️ Final Checklist before you Push:

1.  **Paste the code above** into your `README.md` file in Eclipse.
2.  **Create the folder** named `screenshots` inside your project.
3.  **Put your images** (`dashboard_preview.png`, `booking.png`, `bill.png`) inside that folder.
4.  **Commit and Push** everything to GitHub.

Your project page will now look amazing!
