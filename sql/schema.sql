-- Bus Reservation System - MySQL / MariaDB schema
-- (The application also runs these statements automatically on start-up.)
CREATE DATABASE IF NOT EXISTS bus_reservation;
USE bus_reservation;

CREATE TABLE IF NOT EXISTS users (
    user_id        VARCHAR(10)  PRIMARY KEY,
    role           VARCHAR(10)  NOT NULL,            -- Passenger / Admin
    name           VARCHAR(50)  NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    phone          VARCHAR(10)  NOT NULL,
    password_hash  CHAR(64)     NOT NULL,            -- SHA-256, never plain text
    age            INT          NULL,                -- Passenger only
    loyalty_points INT          NOT NULL DEFAULT 0,  -- Passenger only
    department     VARCHAR(50)  NULL,                -- Admin only
    access_level   INT          NULL                 -- Admin only (1-3)
);

CREATE TABLE IF NOT EXISTS buses (
    bus_no          VARCHAR(15)   PRIMARY KEY,
    bus_type        VARCHAR(10)   NOT NULL,          -- AC / NonAC
    source          VARCHAR(30)   NOT NULL,
    destination     VARCHAR(30)   NOT NULL,
    travel_date     DATE          NOT NULL,
    departure_time  VARCHAR(5)    NOT NULL,
    total_seats     INT           NOT NULL,
    available_seats INT           NOT NULL,
    fare_per_seat   DECIMAL(10,2) NOT NULL,
    CHECK (available_seats >= 0 AND available_seats <= total_seats)
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id        VARCHAR(10)   NOT NULL,
    bus_no         VARCHAR(15)   NOT NULL,
    seats          INT           NOT NULL,
    total_fare     DECIMAL(10,2) NOT NULL,
    status         VARCHAR(10)   NOT NULL,           -- CONFIRMED / CANCELLED
    booked_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_mode   VARCHAR(10)   NOT NULL,
    transaction_id VARCHAR(20)   NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (bus_no)  REFERENCES buses(bus_no)
);
